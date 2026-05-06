import { CommonModule } from '@angular/common';
import { Component, computed, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { catchError, filter, interval, map, of, startWith, switchMap } from 'rxjs';

import { ActivityItem } from '../../../core/models/activity.models';
import { NotificationResponse } from '../../../core/models/notification.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SessionService } from '../../../core/services/session.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle.component';
import { UserMenuComponent } from '../user-menu/user-menu.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink, UserMenuComponent, ThemeToggleComponent],
  template: `
    <header class="topbar">
      <div class="topbar__title">
        <button type="button" class="ghost-button topbar__toggle" (click)="menuToggle.emit()">
          <span class="material-symbols-rounded">menu</span>
        </button>
        <span class="eyebrow">HireConnect workspace</span>
        <h2>{{ title() }}</h2>
      </div>

      <div class="topbar__actions">
        <app-theme-toggle />
        <a class="ghost-button" routerLink="/">
          <span class="material-symbols-rounded">explore</span>
          Explore platform
          <span class="material-symbols-rounded">arrow_forward</span>
        </a>
        <div class="notification-popover">
          <button type="button" class="ghost-button icon-button notification-popover__trigger" (click)="toggleNotifications()">
            <span class="material-symbols-rounded">notifications</span>
            @if (unreadCount()) {
              <span class="notification-dot">{{ unreadCount() }}</span>
            }
          </button>
          @if (notificationsOpen()) {
            <section class="notification-popover__panel card-shell">
              <div class="notification-popover__head">
                <strong>Notifications</strong>
                <button type="button" class="ghost-button" (click)="markAllRead()">Mark all read</button>
              </div>
              @if (notifications().length) {
                @for (item of notifications().slice(0, 5); track item.id) {
                  <button type="button" class="notification-popover__item" [class.is-unread]="!item.isRead" (click)="openNotification(item)">
                    <span class="material-symbols-rounded">{{ item.isRead ? 'notifications' : 'notifications_active' }}</span>
                    <span>
                      <strong>{{ item.type || 'Update' }}</strong>
                      <small>{{ item.message }}</small>
                    </span>
                  </button>
                }
              } @else {
                <p class="notification-popover__empty">No notifications yet.</p>
              }
            </section>
          }
        </div>
        <app-user-menu />
      </div>
    </header>
  `,
})
export class TopbarComponent {
  readonly menuToggle = output<void>();
  protected readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly notificationsService = inject(NotificationService);
  private readonly toast = inject(ToastService);
  protected readonly notifications = signal<ActivityItem[]>([]);
  protected readonly notificationsOpen = signal(false);
  private readonly lastUnreadIds = signal<Set<string>>(new Set());
  private hasLoadedNotifications = false;
  private currentProfileId: number | null = null;

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly title = computed(() => {
    const url = this.currentUrl();
    if (url.includes('/candidate/')) return 'Candidate workspace';
    if (url.includes('/recruiter/')) return 'Recruiter command center';
    if (url.includes('/admin/')) return 'Admin operations hub';
    return 'HireConnect portal';
  });

  protected readonly unreadCount = computed(() => this.notifications().filter((item) => !item.isRead).length);

  constructor() {
    interval(30000).pipe(
      startWith(0),
      switchMap(() => this.viewerProfile.getCurrentProfile(false).pipe(catchError(() => of(null)))),
      switchMap((profile) => {
        this.currentProfileId = profile?.profileId ?? null;
        if (!profile) {
          return of([] as ActivityItem[]);
        }
        return this.notificationsService.getByUser(profile.profileId).pipe(
          map((items) => items.map((item) => this.toActivityItem(item))),
          catchError(() => of([] as ActivityItem[])),
        );
      }),
    ).subscribe((items) => {
      const unread = items.filter((item) => !item.isRead);
      const previous = this.lastUnreadIds();
      const fresh = unread.find((item) => !previous.has(item.id));
      this.notifications.set(items);
      this.lastUnreadIds.set(new Set(unread.map((item) => item.id)));
      if (fresh && this.hasLoadedNotifications) {
        this.toast.info('New notification', fresh.message);
      }
      this.hasLoadedNotifications = true;
    });
  }

  protected toggleNotifications(): void {
    this.notificationsOpen.update((open) => !open);
  }

  protected openNotification(item: ActivityItem): void {
    if (!item.isRead && item.source === 'notification' && item.notificationId) {
      this.notificationsService.markAsRead(item.notificationId).subscribe(() => {
        this.notifications.update((items) => items.map((current) => current.id === item.id ? { ...current, isRead: true } : current));
      });
    }
    const role = this.session.user()?.role?.toLowerCase();
    if (role === 'candidate' || role === 'recruiter') {
      this.router.navigate([`/${role}/notifications`]);
    }
    this.notificationsOpen.set(false);
  }

  protected markAllRead(): void {
    this.viewerProfile.getCurrentProfile(true).pipe(
      switchMap((profile) => {
        if (!profile) {
          return of(undefined);
        }
        return this.notificationsService.markAllRead(profile.profileId).pipe(catchError(() => of(undefined)));
      }),
    ).subscribe(() => {
      this.notifications.update((items) => items.map((item) => ({ ...item, isRead: true })));
      this.notificationsOpen.set(false);
    });
  }

  private toActivityItem(notification: NotificationResponse): ActivityItem {
    const type = notification.type || 'Update';
    return {
      id: `notification-${notification.notificationId}`,
      source: 'notification',
      notificationId: notification.notificationId,
      type,
      title: this.titleForType(type),
      message: notification.message,
      status: notification.isRead ? 'READ' : 'UNREAD',
      createdAt: notification.createdAt,
      isRead: notification.isRead,
      actionLabel: this.actionLabelForType(type),
    };
  }

  private titleForType(type: string): string {
    const normalized = type.toUpperCase();
    if (normalized.includes('INTERVIEW')) return 'Interview update';
    if (normalized.includes('APPLICATION') || normalized.includes('PIPELINE')) return 'Application update';
    if (normalized.includes('OFFER')) return 'Offer update';
    return 'HireConnect update';
  }

  private actionLabelForType(type: string): string {
    return type.toUpperCase().includes('INTERVIEW') ? 'Manage interviews' : 'Open notifications';
  }
}
