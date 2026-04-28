import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';

import { ActivityFeedService } from '../../../core/services/activity-feed.service';
import { ApplicationService } from '../../../core/services/application.service';
import { InterviewService } from '../../../core/services/interview.service';
import { JobService } from '../../../core/services/job.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface CandidateDashboardData {
  jobs: { jobId: number; title: string; category: string; location: string; status: string }[];
  unreadNotifications: number;
  applications?: unknown[];
  interviewCount?: number;
}

@Component({
  selector: 'app-candidate-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatCardComponent, EmptyStateComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Candidate dashboard"
        title="Your career pipeline at a glance"
        description="See open opportunities, active applications, interviews, and unread activity from one premium workspace."
        actionLabel="Browse jobs"
        actionLink="/candidate/jobs"
      />

      @if (!hasProfile()) {
        <app-empty-state
          icon="PF"
          title="Create your profile first"
          description="Your candidate profile unlocks job applications, interview tracking, and richer recruiter discovery."
          actionLabel="Open profile"
          actionLink="/candidate/profile"
        />
      } @else {
        <section class="stats-grid">
          <app-stat-card icon="JB" label="Open jobs" [value]="stats().jobs" caption="Pulled from job-service" />
          <app-stat-card icon="AP" label="Applications" [value]="stats().applications" caption="Live application count" />
          <app-stat-card icon="IV" label="Interviews" [value]="stats().interviews" caption="Across your pipeline" />
          <app-stat-card icon="NT" label="Unread alerts" [value]="stats().unreadNotifications" caption="Notification center" />
        </section>

        <section class="grid-two">
          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">Recommended next step</span>
                <h2>Fresh openings</h2>
              </div>
              <a class="ghost-button" routerLink="/candidate/jobs">See all jobs</a>
            </div>
            @if (recentJobs().length) {
              <div class="surface-list">
                @for (job of recentJobs(); track job.jobId) {
                  <article>
                    <div class="page-header">
                      <div>
                        <h3>{{ job.title }}</h3>
                        <p>{{ job.category }} in {{ job.location }}</p>
                      </div>
                      <app-status-pill [label]="job.status" />
                    </div>
                    <div class="button-row">
                      <a class="ghost-button" [routerLink]="['/candidate/jobs', job.jobId]">View job</a>
                    </div>
                  </article>
                }
              </div>
            } @else {
              <app-empty-state icon="JB" title="No jobs yet" description="Once open roles are available, they will show up here." />
            }
          </article>

          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">Quick actions</span>
                <h2>Keep the pipeline moving</h2>
              </div>
            </div>
            <div class="surface-list">
              <article>
                <h3>Refresh your profile</h3>
                <p>Sharper profile data helps recruiters understand your fit faster.</p>
                <a class="ghost-button" routerLink="/candidate/profile">Edit profile</a>
              </article>
              <article>
                <h3>Review interviews</h3>
                <p>Confirm upcoming interviews or send a reschedule request.</p>
                <a class="ghost-button" routerLink="/candidate/interviews">Open interviews</a>
              </article>
              <article>
                <h3>Stay on top of alerts</h3>
                <p>Unread notifications highlight hiring movement across your applications.</p>
                <a class="ghost-button" routerLink="/candidate/notifications">Open notifications</a>
              </article>
            </div>
          </article>
        </section>
      }
    </section>
  `,
})
export class CandidateDashboardPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly jobs = inject(JobService);
  private readonly applications = inject(ApplicationService);
  private readonly interviews = inject(InterviewService);
  private readonly activityFeed = inject(ActivityFeedService);

  protected readonly hasProfile = signal(false);
  protected readonly stats = signal({ jobs: 0, applications: 0, interviews: 0, unreadNotifications: 0 });
  protected readonly recentJobs = signal<{ jobId: number; title: string; category: string; location: string; status: string }[]>([]);

  constructor() {
    this.load();
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().pipe(
      switchMap((profile) => {
        this.hasProfile.set(!!profile);
        if (!profile) {
          return forkJoin({
            jobs: this.jobs.getJobs({ status: 'OPEN' }).pipe(catchError(() => of([]))),
            unreadNotifications: of(0),
          });
        }

        return forkJoin({
          jobs: this.jobs.getJobs({ status: 'OPEN' }).pipe(catchError(() => of([]))),
          activity: this.activityFeed.getCandidateFeed(profile.profileId).pipe(catchError(() => of([]))),
          applications: this.applications.getByCandidate(profile.profileId).pipe(catchError(() => of([]))),
        }).pipe(
          switchMap((result) => {
            if (!result.applications.length) {
              return of({ ...result, unreadNotifications: result.activity.filter((item) => !item.isRead).length, interviewCount: 0 });
            }
            return forkJoin(
              result.applications.map((application) =>
                this.interviews.getByApplication(application.applicationId).pipe(catchError(() => of([]))),
              ),
            ).pipe(
              switchMap((allInterviews) =>
                of({
                  ...result,
                  unreadNotifications: result.activity.filter((item) => !item.isRead).length,
                  interviewCount: allInterviews.flat().length,
                }),
              ),
            );
          }),
        );
      }),
    ).subscribe((result: CandidateDashboardData) => {
      this.recentJobs.set((result.jobs ?? []).slice(0, 4));
      this.stats.set({
        jobs: (result.jobs ?? []).length,
        applications: Array.isArray(result.applications) ? result.applications.length : 0,
        interviews: typeof result.interviewCount === 'number' ? result.interviewCount : 0,
        unreadNotifications: result.unreadNotifications ?? 0,
      });
    });
  }
}
