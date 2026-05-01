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
  imports: [CommonModule, RouterLink, StatCardComponent, EmptyStateComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <section class="workspace-hero">
        <div class="workspace-hero__copy">
          <p class="muted">Good evening, {{ userName() }}</p>
          <span class="eyebrow">Candidate dashboard</span>
          <h1>Your career pipeline at a glance</h1>
          <p class="muted">Track your opportunities, stay updated on interviews, and keep your job search moving forward.</p>
        </div>
        <a class="primary-button" routerLink="/candidate/jobs">
          Browse jobs
          <span class="material-symbols-rounded">arrow_forward</span>
        </a>
      </section>

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
          <app-stat-card icon="JB" label="Open jobs" [value]="stats().jobs" caption="Fresh roles in your feed" />
          <app-stat-card icon="AP" label="Active applications" [value]="stats().applications" caption="Roles you've applied for" />
          <app-stat-card icon="IV" label="Interviews scheduled" [value]="stats().interviews" caption="Upcoming interviews" />
          <app-stat-card icon="NT" label="Unread alerts" [value]="stats().unreadNotifications" caption="Important updates" />
        </section>

        <section class="grid-two">
          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Recommended for you</span>
                <h2>Curated roles that match your profile</h2>
              </div>
              <a class="ghost-button" routerLink="/candidate/jobs">See all jobs</a>
            </div>
            @if (recentJobs().length) {
              <div class="surface-list">
                @for (job of recentJobs(); track job.jobId) {
                  <article class="list-row">
                    <div class="job-market-card__top">
                      <span class="company-mark">{{ companyMark(job.title) }}</span>
                      <div>
                        <h3>{{ job.title }}</h3>
                        <p>{{ job.category }} in {{ job.location }}</p>
                      </div>
                    </div>
                    <div class="button-row">
                      <app-status-pill [label]="job.status" />
                      <a class="ghost-button" [routerLink]="['/candidate/jobs', job.jobId]">
                        View job
                        <span class="material-symbols-rounded">arrow_forward</span>
                      </a>
                    </div>
                  </article>
                }
              </div>
            } @else {
              <app-empty-state icon="JB" title="No jobs yet" description="Once open roles are available, they will show up here." />
            }
          </article>

          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Quick actions</span>
                <h2>Everything you need to stay ahead</h2>
              </div>
            </div>
            <div class="quick-action-grid">
              <a class="quick-action" routerLink="/candidate/profile">
                <span class="material-symbols-rounded">manage_accounts</span>
                <div>
                  <strong>Update profile</strong>
                  <small>Keep your profile fresh</small>
                </div>
              </a>
              <a class="quick-action" routerLink="/candidate/profile">
                <span class="material-symbols-rounded">upload</span>
                <div>
                  <strong>Upload resume</strong>
                  <small>Improve visibility</small>
                </div>
              </a>
              <a class="quick-action" routerLink="/candidate/applications">
                <span class="material-symbols-rounded">fact_check</span>
                <div>
                  <strong>Track applications</strong>
                  <small>Monitor your pipeline</small>
                </div>
              </a>
              <a class="quick-action" routerLink="/candidate/bookmarks">
                <span class="material-symbols-rounded">bookmark_search</span>
                <div>
                  <strong>Saved searches</strong>
                  <small>View saved roles</small>
                </div>
              </a>
            </div>
          </article>
        </section>

        <section class="grid-two">
          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Saved roles</span>
                <h2>Roles you've saved for later</h2>
              </div>
              <a class="ghost-button" routerLink="/candidate/bookmarks">View all</a>
            </div>
            <div class="surface-list">
              <article class="list-row">
                <div class="job-market-card__top">
                  <span class="company-mark">HC</span>
                  <div>
                    <h3>Frontend Developer</h3>
                    <p>DreamSports | Gurugram</p>
                  </div>
                </div>
                <strong>Saved recently</strong>
              </article>
            </div>
          </article>

          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Career boost</span>
                <h2>Keep your profile current</h2>
                <p>Update your resume, skills, and contact details so recruiters see accurate information.</p>
              </div>
              <a class="primary-button" routerLink="/candidate/profile">Continue</a>
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
  protected readonly userName = signal('Tester');

  constructor() {
    this.load();
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().pipe(
      switchMap((profile) => {
        this.hasProfile.set(!!profile);
        this.userName.set(profile?.fullName?.split(' ')[0] ?? 'Tester');
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
