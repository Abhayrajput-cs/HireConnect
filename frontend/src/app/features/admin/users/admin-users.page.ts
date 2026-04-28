import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { ProfileResponse } from '../../../core/models/profile.models';
import { ProfileService } from '../../../core/services/profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Users"
        title="Read-only user inventory"
        description="The backend does not expose suspend or admin user-management commands, so this view stays contract-safe and read-only."
      />

      @if (users().length) {
        <table class="data-table card-shell content-card">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Company</th>
            </tr>
          </thead>
          <tbody>
            @for (user of users(); track user.profileId) {
              <tr>
                <td>{{ user.fullName || '-' }}</td>
                <td>{{ user.email }}</td>
                <td><app-status-pill [label]="user.role" /></td>
                <td>{{ user.companyName || '-' }}</td>
              </tr>
            }
          </tbody>
        </table>
      } @else {
        <app-empty-state icon="US" title="No users visible" description="Profiles will appear here once the admin session can access the profile index." />
      }
    </section>
  `,
})
export class AdminUsersPageComponent {
  private readonly profiles = inject(ProfileService);
  protected readonly users = signal<ProfileResponse[]>([]);

  constructor() {
    this.profiles.getProfiles().subscribe((profiles) => this.users.set(profiles));
  }
}
