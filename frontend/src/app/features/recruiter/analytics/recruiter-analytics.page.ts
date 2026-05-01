import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { AnalyticsSummary } from '../../../core/models/analytics.models';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { ApplicationService } from '../../../core/services/application.service';
import { JobService } from '../../../core/services/job.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

interface JobMetricView {
  title: string;
  viewCount: number;
  applicationCount: number;
  ratio: string;
}

@Component({
  selector: 'app-recruiter-analytics-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatCardComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Recruiter analytics"
        title="Measure demand and pipeline quality"
        description="These recruiter metrics come from the merged notification and analytics service."
      />

      @if (summary()) {
        <section class="stats-grid">
          <app-stat-card icon="JB" label="Total jobs" [value]="summary()!.totalJobs" />
          <app-stat-card icon="AP" label="Applications" [value]="summary()!.totalApplications" />
          <app-stat-card icon="SL" label="Shortlisted" [value]="summary()!.shortlistedCount" />
        </section>

        <section class="grid-two">
          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">Per-job metrics</span>
                <h2>Demand by posting</h2>
              </div>
            </div>
            @if (jobMetrics().length) {
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Views</th>
                    <th>Applications</th>
                    <th>Ratio</th>
                  </tr>
                </thead>
                <tbody>
                  @for (metric of jobMetrics(); track metric.title) {
                    <tr>
                      <td>{{ metric.title }}</td>
                      <td>{{ metric.viewCount }}</td>
                      <td>{{ metric.applicationCount }}</td>
                      <td>{{ metric.ratio }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            } @else {
              <app-empty-state icon="AN" title="No recruiter metrics yet" description="Once jobs collect views and applications, this table will populate." />
            }
          </article>

          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">Category demand</span>
                <h2>Top categories</h2>
              </div>
            </div>
            <div class="surface-list">
              @for (entry of topCategories(); track entry.key) {
                <article>
                  <h3>{{ entry.key }}</h3>
                  <p>{{ entry.value }} tracked events</p>
                </article>
              }
            </div>
          </article>
        </section>
      } @else {
        <app-empty-state icon="AN" title="Analytics unavailable" description="Recruiter analytics will appear once your recruiter profile exists and jobs start collecting traffic." />
      }
    </section>
  `,
})
export class RecruiterAnalyticsPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly analytics = inject(AnalyticsService);
  private readonly jobs = inject(JobService);
  private readonly applications = inject(ApplicationService);

  protected readonly summary = signal<AnalyticsSummary | null>(null);
  protected readonly jobMetrics = signal<JobMetricView[]>([]);
  protected readonly topCategories = signal<{ key: string; value: number }[]>([]);

  constructor() {
    this.load();
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().subscribe((profile) => {
      if (!profile) {
        this.summary.set(null);
        this.jobMetrics.set([]);
        this.topCategories.set([]);
        return;
      }

      forkJoin({
        jobs: this.jobs.getJobsByRecruiter(profile.profileId).pipe(catchError(() => of([]))),
      }).pipe(
        switchMap((result) => {
          const categories = result.jobs.reduce<Record<string, number>>((acc, job) => {
            acc[job.category] = (acc[job.category] ?? 0) + 1;
            return acc;
          }, {});

          if (!result.jobs.length) {
            return of({
              summary: {
                totalJobs: 0,
                totalApplications: 0,
                shortlistedCount: 0,
                offeredCount: 0,
                rejectedCount: 0,
                avgTimeToHireDays: 0,
                viewToApplyRatio: 0,
              } satisfies AnalyticsSummary,
              metrics: [] as JobMetricView[],
              topCategories: categories,
            });
          }

          return forkJoin(
            result.jobs.map((job) =>
              forkJoin({
                applications: this.applications.getByJob(job.jobId).pipe(catchError(() => of([]))),
                viewCount: this.analytics.getJobViewCount(job.jobId).pipe(catchError(() => of(0))),
              }).pipe(
                map(({ applications, viewCount }) => {
                  const applicationCount = applications.length;
                  const shortlistedCount = applications.filter((application) => application.status === 'SHORTLISTED').length;
                  const offeredCount = applications.filter((application) => application.status === 'OFFERED').length;
                  const rejectedCount = applications.filter((application) => application.status === 'REJECTED').length;
                  return {
                    title: job.title,
                    category: job.category,
                    viewCount,
                    applicationCount,
                    shortlistedCount,
                    offeredCount,
                    rejectedCount,
                    ratio: applicationCount ? (viewCount / applicationCount).toFixed(2) : '0.00',
                  };
                }),
              ),
            ),
          ).pipe(
            map((metrics) => {
              const totalApplications = metrics.reduce((sum, metric) => sum + metric.applicationCount, 0);
              const totalViews = metrics.reduce((sum, metric) => sum + metric.viewCount, 0);
              const shortlistedCount = metrics.reduce((sum, metric) => sum + metric.shortlistedCount, 0);
              const offeredCount = metrics.reduce((sum, metric) => sum + metric.offeredCount, 0);
              const rejectedCount = metrics.reduce((sum, metric) => sum + metric.rejectedCount, 0);
              return {
                summary: {
                  totalJobs: result.jobs.length,
                  totalApplications,
                  shortlistedCount,
                  offeredCount,
                  rejectedCount,
                  avgTimeToHireDays: 0,
                  viewToApplyRatio: totalApplications ? totalViews / totalApplications : 0,
                } satisfies AnalyticsSummary,
                metrics: metrics.map((metric) => ({
                  title: metric.title,
                  viewCount: metric.viewCount,
                  applicationCount: metric.applicationCount,
                  ratio: metric.ratio,
                })),
                topCategories: categories,
              };
            }),
          );
        }),
      ).subscribe((result) => {
        this.summary.set(result.summary);
        this.jobMetrics.set(result.metrics);
        this.topCategories.set(Object.entries(result.topCategories).map(([key, value]) => ({ key, value })));
      });
    });
  }
}
