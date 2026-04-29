import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { AnalyticsSummary } from '../../../core/models/analytics.models';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-analytics-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatCardComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Platform analytics"
        title="Aggregate platform metrics"
        description="Admin analytics are rendered only from the endpoints the backend currently exposes."
      />

      @if (summary()) {
        <section class="stats-grid">
          <app-stat-card icon="JB" label="Total jobs" [value]="summary()!.totalJobs" />
          <app-stat-card icon="AP" label="Applications" [value]="summary()!.totalApplications" />
          <app-stat-card icon="RJ" label="Rejected" [value]="summary()!.rejectedCount" />
          <app-stat-card icon="VR" label="View/apply ratio" [value]="summary()!.viewToApplyRatio.toFixed(2)" />
        </section>

        <article class="workspace-panel">
          <div class="page-header">
            <div>
              <span class="eyebrow">Top categories</span>
              <h2>Demand concentration</h2>
            </div>
          </div>
          <div class="surface-list">
            @for (entry of categories(); track entry.key) {
              <article class="list-row">
                <div class="job-market-card__top">
                  <span class="soft-icon material-symbols-rounded">query_stats</span>
                  <div>
                    <h3>{{ entry.key }}</h3>
                    <p>{{ entry.value }} tracked events</p>
                  </div>
                </div>
              </article>
            }
          </div>
        </article>
      } @else {
        <app-empty-state icon="AN" title="Admin analytics unavailable" [description]="errorMessage() || 'The backend did not return platform analytics for the current session.'" />
      }
    </section>
  `,
})
export class AdminAnalyticsPageComponent {
  private readonly analytics = inject(AnalyticsService);

  protected readonly summary = signal<AnalyticsSummary | null>(null);
  protected readonly categories = signal<{ key: string; value: number }[]>([]);
  protected readonly errorMessage = signal('');

  constructor() {
    this.analytics.getPlatformStats().subscribe({
      next: (summary) => this.summary.set(summary),
      error: (error: unknown) => this.errorMessage.set(getErrorMessage(error, 'Unable to load platform analytics.')),
    });

    this.analytics.getTopCategories().subscribe({
      next: (categories) => this.categories.set(Object.entries(categories).map(([key, value]) => ({ key, value }))),
      error: () => undefined,
    });
  }
}
