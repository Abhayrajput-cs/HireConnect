import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { AnalyticsSummary } from '../../../core/models/analytics.models';
import { AdminService } from '../../../core/services/admin.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatCardComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Admin dashboard"
        title="Platform-level command center"
        description="Read-only admin views are driven by the analytics and profile endpoints exposed by the backend."
      />

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
