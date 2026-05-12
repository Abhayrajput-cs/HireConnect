import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { AnalyticsSummary } from '../../../core/models/analytics.models';
import { AdminService } from '../../../core/services/admin.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, StatCardComponent],
  template: `
    <section class="page-section">
      <section class="workspace-hero">
        <div class="workspace-hero__copy">
          <span class="eyebrow">Admin dashboard</span>
          <h1>Platform-level command center</h1>
          <p class="muted">Monitor users, jobs, applications, and hiring outcomes across the HireConnect operating system.</p>
        </div>
        <span class="soft-icon material-symbols-rounded">admin_panel_settings</span>
      </section>

      @if (analytics()) {
        <section class="stats-grid">
          <app-stat-card icon="JB" label="Total jobs" [value]="analytics()!.totalJobs" />
          <app-stat-card icon="AP" label="Total applications" [value]="analytics()!.totalApplications" />
          <app-stat-card icon="SL" label="Shortlisted" [value]="analytics()!.shortlistedCount" />
          <app-stat-card icon="OF" label="Offered" [value]="analytics()!.offeredCount" />
        </section>
      } @else {
        <app-empty-state icon="AD" title="Admin analytics unavailable" [description]="errorMessage() || 'This backend only exposes admin data when an ADMIN session is available.'" />
      }
    </section>
  `,
})
export class AdminDashboardPageComponent {
  private readonly admin = inject(AdminService);

  protected readonly analytics = signal<AnalyticsSummary | null>(null);
  protected readonly errorMessage = signal('');

  constructor() {
    this.admin.getDashboardData().subscribe({
      next: (result) => this.analytics.set(result.analytics),
      error: (error: unknown) => this.errorMessage.set(getErrorMessage(error, 'Unable to load admin dashboard.')),
    });
  }
}
