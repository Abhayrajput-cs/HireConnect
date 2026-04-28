import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';

import { ActivityItem } from '../../../core/models/activity.models';
import { ActivityFeedService } from '../../../core/services/activity-feed.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-candidate-notifications-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Notifications"
        title="Stay on top of every hiring signal"
        description="Read state is synced against the notification service, including bulk mark-as-read."
      />

      <div class="button-row">
        <button class="ghost-button" type="button" (click)="load()">Refresh</button>
        <button class="primary-button" type="button" (click)="markAllRead()">Mark all alerts read</button>
      </div>

      @if (activity().length) {
        <div class="activity-feed">
          @for (item of activity(); track item.id) {
            <article class="card-shell content-card activity-card">
              <div class="page-header">
                <div class="activity-card__meta">
                  <span class="activity-card__icon material-symbols-rounded">{{ iconFor(item.type) }}</span>
                  <div>
                    <span class="eyebrow">{{ item.type }}</span>
                    <h2>{{ item.title }}</h2>
                    <p>{{ item.message }}</p>
                    <small>{{ item.createdAt | date:'medium' }}</small>
                  </div>
                </div>
                <app-status-pill [label]="item.isRead ? 'READ' : item.status" />
              </div>
              <div class="button-row">
                @if (item.actionLink) {
                  <a class="ghost-button" [routerLink]="item.actionLink">{{ item.actionLabel || 'Open' }}</a>
                }
                @if (!item.isRead) {
                  <button class="ghost-button" type="button" (click)="markRead(item)">Mark read</button>
                }
                <button class="danger-button" type="button" (click)="remove(item)">Dismiss</button>
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="NT" title="No notifications yet" description="Alerts for application status changes and interviews will appear here." />
      }
    </section>
  `,
})
export class CandidateNotificationsPageComponent {
  private readonly notificationsService = inject(NotificationService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly activityFeed = inject(ActivityFeedService);
  private readonly toast = inject(ToastService);

  protected readonly activity = signal<ActivityItem[]>([]);
  private profileId: number | null = null;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.viewerProfile.getCurrentProfile().pipe(
      switchMap((profile) => {
        this.profileId = profile?.profileId ?? null;
        if (!this.profileId) {
          return of<ActivityItem[]>([]);
        }
        return this.activityFeed.getCandidateFeed(this.profileId);
      }),
    ).subscribe({
      next: (activity) => this.activity.set(activity),
      error: () => {
        this.activity.set([]);
        this.toast.error('Notifications unavailable', 'Unable to load candidate notifications right now.');
      },
    });
  }

  protected markRead(item: ActivityItem): void {
    if (!this.profileId) {
      return;
    }

    if (item.source === 'notification' && item.notificationId) {
      this.notificationsService.markAsRead(item.notificationId).subscribe({
        next: () => this.load(),
        error: () => this.toast.error('Update failed', 'Unable to mark this notification as read.'),
      });
      return;
    }

    this.activityFeed.markDerivedAsRead('candidate', this.profileId, item.id);
    this.load();
  }

  protected markAllRead(): void {
    if (!this.profileId) {
      return;
    }

    const derivedIds = this.activity()
      .filter((item) => item.source === 'derived')
      .map((item) => item.id);

    forkJoin({
      notifications: this.notificationsService.markAllRead(this.profileId),
      derived: of(this.activityFeed.markAllDerivedAsRead('candidate', this.profileId, derivedIds)),
    }).subscribe({
      next: () => this.load(),
      error: () => this.toast.error('Update failed', 'Unable to mark all candidate notifications as read.'),
    });
  }

  protected remove(item: ActivityItem): void {
    if (!this.profileId) {
      return;
    }

    if (item.source === 'notification' && item.notificationId) {
      this.notificationsService.deleteNotification(item.notificationId).subscribe({
        next: () => this.load(),
        error: () => this.toast.error('Delete failed', 'Unable to remove this notification right now.'),
      });
      return;
    }

    this.activityFeed.dismissDerived('candidate', this.profileId, item.id);
    this.load();
  }

  protected iconFor(type: string): string {
    switch (type) {
      case 'INTERVIEW':
        return 'calendar_month';
      case 'APPLICATION':
      case 'PIPELINE':
        return 'description';
      default:
        return 'notifications';
    }
  }
}
