import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, Observable, of, switchMap } from 'rxjs';

import { ActivityItem } from '../../../core/models/activity.models';
import { ActivityFeedService } from '../../../core/services/activity-feed.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';

import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-recruiter-notifications-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    EmptyStateComponent,
    PageHeaderComponent,
    StatusPillComponent,
  ],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Recruiter notifications"
        title="Follow pipeline events without losing momentum"
        description="Read, clear, and manage recruiter alerts from the merged notification service."
      />

      <div class="button-row">
        <button class="ghost-button" type="button" (click)="load()">
          Refresh
        </button>

        <button
          class="primary-button"
          type="button"
          (click)="markAllRead()"
        >
          Mark all alerts read
        </button>
      </div>

      @if (activity().length) {
        <div class="activity-feed">
          @for (item of activity(); track item.id) {
            <article class="card-shell content-card activity-card">
              <div class="page-header">
                <div class="activity-card__meta">
                  <span class="activity-card__icon material-symbols-rounded">
                    {{ iconMap[item.type] || 'notifications' }}
                  </span>

                  <div>
                    <span class="eyebrow">{{ item.type }}</span>
                    <h2>{{ item.title }}</h2>
                    <p>{{ item.message }}</p>
                    <small>{{ item.createdAt | date:'medium' }}</small>
                  </div>
                </div>

                <app-status-pill
                  [label]="item.isRead ? 'READ' : item.status"
                />
              </div>

              <div class="button-row">
                @if (item.actionLink) {
                  <a
                    class="ghost-button"
                    [routerLink]="item.actionLink"
                  >
                    {{ item.actionLabel || 'Open' }}
                  </a>
                }

                @if (!item.isRead) {
                  <button
                    class="ghost-button"
                    type="button"
                    (click)="markRead(item)"
                  >
                    Mark read
                  </button>
                }

                <button
                  class="danger-button"
                  type="button"
                  (click)="remove(item)"
                >
                  Dismiss
                </button>
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state
          icon="NT"
          title="No recruiter notifications yet"
          description="Alerts triggered by applications and interviews will appear here."
        />
      }
    </section>
  `,
})
export class RecruiterNotificationsPageComponent {
  private readonly notificationsService = inject(NotificationService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly activityFeed = inject(ActivityFeedService);
  private readonly toast = inject(ToastService);

  protected readonly activity = signal<ActivityItem[]>([]);

  private profileId: number | null = null;

  protected readonly iconMap: Record<string, string> = {
    INTERVIEW: 'calendar_month',
    APPLICATION: 'description',
    PIPELINE: 'description',
  };

  constructor() {
    this.load();
  }

  protected load(): void {
    this.viewerProfile
      .getCurrentProfile()
      .pipe(
        switchMap((profile) => {
          this.profileId = profile?.profileId ?? null;

          return this.profileId
            ? this.activityFeed.getRecruiterFeed(this.profileId)
            : of<ActivityItem[]>([]);
        })
      )
      .subscribe({
        next: (activity) => this.activity.set(activity),
        error: () => {
          this.activity.set([]);
          this.showError(
            'Notifications unavailable',
            'Unable to load recruiter notifications right now.'
          );
        },
      });
  }

  protected markRead(item: ActivityItem): void {
    this.executeAction(
      this.getReadRequest(item),
      'Update failed',
      'Unable to mark this notification as read.'
    );
  }

  protected markAllRead(): void {
    if (!this.profileId) {
      return;
    }

    const derivedIds = this.activity()
      .filter((item) => item.source === 'derived')
      .map((item) => item.id);

    this.executeAction(
      forkJoin({
        notifications: this.notificationsService.markAllRead(this.profileId),
        derived: of(
          this.activityFeed.markAllDerivedAsRead(
            'recruiter',
            this.profileId,
            derivedIds
          )
        ),
      }),
      'Update failed',
      'Unable to mark all recruiter notifications as read.'
    );
  }

  protected remove(item: ActivityItem): void {
    this.executeAction(
      this.getRemoveRequest(item),
      'Delete failed',
      'Unable to remove this notification right now.'
    );
  }

  private getReadRequest(item: ActivityItem): Observable<unknown> {
    if (!this.profileId) {
      return of(null);
    }

    return item.source === 'notification' && item.notificationId
      ? this.notificationsService.markAsRead(item.notificationId)
      : of(
          this.activityFeed.markDerivedAsRead(
            'recruiter',
            this.profileId,
            item.id
          )
        );
  }

  private getRemoveRequest(item: ActivityItem): Observable<unknown> {
    if (!this.profileId) {
      return of(null);
    }

    return item.source === 'notification' && item.notificationId
      ? this.notificationsService.deleteNotification(item.notificationId)
      : of(
          this.activityFeed.dismissDerived(
            'recruiter',
            this.profileId,
            item.id
          )
        );
  }

  private executeAction(
    request$: Observable<unknown>,
    title: string,
    message: string
  ): void {
    request$.subscribe({
      next: () => this.load(),
      error: () => this.showError(title, message),
    });
  }

  private showError(title: string, message: string): void {
    this.toast.error(title, message);
  }
}
