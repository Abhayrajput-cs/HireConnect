import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { ActivityFeedService } from '../../../core/services/activity-feed.service';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { ApplicationService } from '../../../core/services/application.service';
import { JobService } from '../../../core/services/job.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-recruiter-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, StatCardComponent],
  template: `
    <section class="page-section">
      <section class="workspace-hero">
        <div class="workspace-hero__copy">
          <span class="eyebrow">Recruiter dashboard</span>
          <h1>Run your hiring pipeline with confidence</h1>
          <p class="muted">See open jobs, application volume, pipeline quality, and unread activity in one command center.</p>
        </div>
        <a class="primary-button" routerLink="/recruiter/jobs/new">
          Post a new job
          <span class="material-symbols-rounded">arrow_forward</span>
        </a>
      </section>

      @if (!hasProfile()) {
        <app-empty-state
          icon="PF"
          title="Create your recruiter profile first"
          description="Your company profile is required before you can publish and manage jobs."
          actionLabel="Open recruiter profile"
          actionLink="/recruiter/profile"
        />
      } @else {
        <section class="stats-grid">
          <app-stat-card icon="JB" label="Posted jobs" [value]="stats().jobs" caption="Roles owned by your recruiter profile" />
          <app-stat-card icon="AP" label="Applications" [value]="stats().applications" caption="Across your jobs" />
          <app-stat-card icon="AN" label="View/apply ratio" [value]="stats().ratio" caption="Recruiter analytics summary" />
          <app-stat-card icon="NT" label="Unread alerts" [value]="stats().unreadNotifications" caption="Notification service count" />
        </section>

        <section class="grid-two">
          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Quick actions</span>
                <h2>Keep hiring momentum high</h2>
              </div>
            </div>
            <div class="surface-list">
              <article>
                <h3>Create a fresh posting</h3>
                <p>Launch a new role with the exact job-service contract used by the backend.</p>
                <a class="primary-button" routerLink="/recruiter/jobs/new">Post a job</a>
              </article>
              <article>
                <h3>Review applicants</h3>
                <p>Shortlist, reject, and schedule interviews directly from live application data.</p>
                <a class="ghost-button" routerLink="/recruiter/jobs">Manage jobs</a>
              </article>
              <article>
                <h3>Open analytics</h3>
                <p>Measure view counts, application rates, and time-to-hire from the merged analytics service.</p>
                <a class="ghost-button" routerLink="/recruiter/analytics">View analytics</a>
              </article>
            </div>
          </article>

          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Current pulse</span>
                <h2>Active job slots</h2>
              </div>
              <a class="ghost-button" routerLink="/recruiter/jobs">Open jobs</a>
            </div>
            @if (latestJobs().length) {
              <div class="surface-list">
                @for (job of latestJobs(); track job.jobId) {
                  <article class="list-row">
                    <div class="job-market-card__top">
                      <span class="company-mark">{{ companyMark(job.title) }}</span>
                      <div>
                        <h3>{{ job.title }}</h3>
                        <p>{{ job.location }} | {{ job.status }}</p>
                      </div>
                    </div>
                    <a class="ghost-button" [routerLink]="['/recruiter/jobs', job.jobId, 'applicants']">View applicants</a>
                  </article>
                }
              </div>
            } @else {
              <app-empty-state icon="JB" title="No posted jobs yet" description="Once you create job listings, they will appear here with live pipeline stats." />
            }
          </article>
        </section>
      }
    </section>
  `,
})
export class RecruiterDashboardPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly jobs = inject(JobService);
  private readonly applications = inject(ApplicationService);
  private readonly analytics = inject(AnalyticsService);
  private readonly activityFeed = inject(ActivityFeedService);

  protected readonly hasProfile = signal(false);
  protected readonly latestJobs = signal<{ jobId: number; title: string; location: string; status: string }[]>([]);
  protected readonly stats = signal({ jobs: 0, applications: 0, ratio: '0.00', unreadNotifications: 0 });

  constructor() {
    this.load();
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().subscribe((profile) => {
      this.hasProfile.set(!!profile);
      if (!profile) {
        this.latestJobs.set([]);
        this.stats.set({ jobs: 0, applications: 0, ratio: '0.00', unreadNotifications: 0 });
        return;
      }

      forkJoin({
        jobs: this.jobs.getJobsByRecruiter(profile.profileId).pipe(catchError(() => of([]))),
        activity: this.activityFeed.getRecruiterFeed(profile.profileId).pipe(catchError(() => of([]))),
      }).subscribe((result) => {
        this.latestJobs.set(result.jobs.slice(0, 4));
        const unreadNotifications = result.activity.filter((item) => !item.isRead).length;

        if (!result.jobs.length) {
          this.stats.set({
            jobs: 0,
            applications: 0,
            ratio: '0.00',
            unreadNotifications,
          });
          return;
        }

        forkJoin({
          applicationCounts: forkJoin(
            result.jobs.map((job) => this.applications.countByJob(job.jobId).pipe(catchError(() => of(0)))),
          ),
          viewCounts: forkJoin(
            result.jobs.map((job) => this.analytics.getJobViewCount(job.jobId).pipe(catchError(() => of(0)))),
          ),
        }).subscribe((metrics) => {
          const totalApplications = metrics.applicationCounts.reduce((sum, count) => sum + count, 0);
          const totalViews = metrics.viewCounts.reduce((sum, count) => sum + count, 0);
          const ratio = totalApplications ? totalViews / totalApplications : 0;

          this.stats.set({
            jobs: result.jobs.length,
            applications: totalApplications,
            ratio: ratio.toFixed(2),
            unreadNotifications,
          });
        });
      });
    });
  }

  protected companyMark(title: string): string {
    return title
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  }
}
