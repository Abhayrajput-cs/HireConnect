import { CommonModule } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { NavItem } from '../../../core/models/ui.models';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar" [class.sidebar--open]="open()">
      <a class="sidebar__brand" [routerLink]="session.isAuthenticated() ? session.roleHome() : '/'">
        <span class="sidebar__badge">HC</span>
        <div>
          <strong>HireConnect</strong>
          <small>operating system</small>
        </div>
      </a>

      <nav class="sidebar__nav">
        @for (item of items(); track item.route) {
          <a [routerLink]="item.route" routerLinkActive="is-active" class="sidebar__link">
            <span class="material-symbols-rounded">{{ item.icon }}</span>
            <div>
              <strong>{{ item.label }}</strong>
              @if (item.description) {
                <small>{{ item.description }}</small>
              }
            </div>
          </a>
        }
      </nav>
    </aside>
  `,
})
export class SidebarComponent {
  readonly open = input(false);
  protected readonly session = inject(SessionService);

  protected readonly items = computed<NavItem[]>(() => {
    switch (this.session.role()) {
      case 'RECRUITER':
        return [
          { label: 'Dashboard', route: '/recruiter/dashboard', icon: 'space_dashboard', description: 'Pulse and quick actions' },
          { label: 'Profile', route: '/recruiter/profile', icon: 'business_center', description: 'Company identity' },
          { label: 'Manage Jobs', route: '/recruiter/jobs', icon: 'work', description: 'Postings and applicants' },
          { label: 'Interviews', route: '/recruiter/interviews', icon: 'calendar_month', description: 'Rooms and schedules' },
          { label: 'Analytics', route: '/recruiter/analytics', icon: 'monitoring', description: 'Views, ratios, pipeline' },
          { label: 'Notifications', route: '/recruiter/notifications', icon: 'notifications', description: 'Unread and alerts' },
        ];
      case 'ADMIN':
        return [
          { label: 'Dashboard', route: '/admin/dashboard', icon: 'space_dashboard', description: 'Platform command' },
          { label: 'Users', route: '/admin/users', icon: 'groups', description: 'Role inventory' },
          { label: 'Jobs', route: '/admin/jobs', icon: 'work_history', description: 'Catalog overview' },
          { label: 'Analytics', route: '/admin/analytics', icon: 'query_stats', description: 'Platform metrics' },
        ];
      case 'CANDIDATE':
      default:
        return [
          { label: 'Dashboard', route: '/candidate/dashboard', icon: 'space_dashboard', description: 'Career pulse' },
          { label: 'Profile', route: '/candidate/profile', icon: 'badge', description: 'Identity and resume' },
          { label: 'Browse Jobs', route: '/candidate/jobs', icon: 'travel_explore', description: 'Search open roles' },
          { label: 'Applications', route: '/candidate/applications', icon: 'description', description: 'Pipeline history' },
          { label: 'Interviews', route: '/candidate/interviews', icon: 'calendar_month', description: 'Schedules and updates' },
          { label: 'Bookmarks', route: '/candidate/bookmarks', icon: 'bookmark', description: 'Saved roles' },
          { label: 'Notifications', route: '/candidate/notifications', icon: 'notifications_active', description: 'Unread activity' },
        ];
    }
  });
}
