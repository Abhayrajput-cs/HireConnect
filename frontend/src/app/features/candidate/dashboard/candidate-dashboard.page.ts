import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';

import { ApplicationResponse } from '../../../core/models/application.models';
import { InterviewResponse } from '../../../core/models/interview.models';
import { JobResponse } from '../../../core/models/job.models';
import { ActivityFeedService } from '../../../core/services/activity-feed.service';
import { ApplicationService } from '../../../core/services/application.service';
import { InterviewService } from '../../../core/services/interview.service';
import { JobService } from '../../../core/services/job.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface CandidateDashboardData {
  jobs: JobResponse[];
  unreadNotifications: number;
  applications?: ApplicationResponse[];
  pipeline?: PipelineItem[];
  interviewCount?: number;
}

interface PipelineItem {
  application: ApplicationResponse;
  job: JobResponse | null;
  interviews: InterviewResponse[];
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
          <app-stat-card icon="CO" label="Companies applied" [value]="stats().companies" caption="Unique companies in your pipeline" />
          <app-stat-card icon="OF" label="Offers received" [value]="stats().offers" caption="Applications moved to offer" />
          <app-stat-card icon="RJ" label="Rejected" [value]="stats().rejections" caption="Applications closed by recruiters" />
          <app-stat-card icon="IV" label="Interview pipeline" [value]="stats().interviews" caption="Scheduled or confirmed slots" />
        </section>

        <section class="grid-two">
          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Application profile</span>
                <h2>Where your job search stands</h2>
              </div>
              <a class="ghost-button" routerLink="/candidate/applications">View all</a>
            </div>

            @if (pipeline().length) {
              <div class="pipeline-meter">
                <div>
                  <span>Offer rate</span>
                  <strong>{{ stats().offerRate }}%</strong>
                </div>
                <div>
                  <span>Active applications</span>
                  <strong>{{ stats().active }}</strong>
                </div>
                <div>
                  <span>Unread alerts</span>
                  <strong>{{ stats().unreadNotifications }}</strong>
                </div>
              </div>

              <div class="status-breakdown">
                @for (item of statusBreakdown(); track item.label) {
                  <div>
                    <span>{{ item.label }}</span>
                    <strong>{{ item.count }}</strong>
                  </div>
                }
              </div>
            } @else {
              <app-empty-state icon="AP" title="No applications yet" description="Apply to jobs to build your career pipeline dashboard." />
            }
          </article>

          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Companies</span>
                <h2>Companies you have applied to</h2>
              </div>
            </div>
            @if (companySummaries().length) {
              <div class="surface-list">
                @for (company of companySummaries(); track company.name) {
                  <article class="list-row">
                    <div class="job-market-card__top">
                      <span class="company-mark">{{ companyMark(company.name) }}</span>
                      <div>
                        <h3>{{ company.name }}</h3>
                        <p>{{ company.total }} application{{ company.total === 1 ? '' : 's' }}</p>
                      </div>
                    </div>
                    <div class="company-status-strip">
                      <span>{{ company.offers }} offers</span>
                      <span>{{ company.rejections }} rejected</span>
                    </div>
                  </article>
                }
              </div>
            } @else {
              <app-empty-state icon="CO" title="No companies yet" description="Companies will appear here once you start applying." />
            }
          </article>
        </section>

        <section class="workspace-panel">
          <div class="page-header">
            <div>
              <span class="eyebrow">Live pipeline</span>
              <h2>Your applications by company</h2>
            </div>
            <a class="primary-button" routerLink="/candidate/jobs">Apply more</a>
          </div>

          @if (pipeline().length) {
            <div class="surface-list">
              @for (item of pipeline(); track item.application.applicationId) {
                <article class="list-row application-row">
                  <div class="job-market-card__top">
                    <span class="company-mark">{{ companyMark(companyName(item)) }}</span>
                    <div>
                      <h3>{{ item.job?.title || 'Role unavailable' }}</h3>
                      <p>{{ companyName(item) }} | {{ item.job?.location || 'Location unavailable' }}</p>
                      <small>Applied {{ item.application.appliedAt | date:'mediumDate' }}</small>
                    </div>
                  </div>
                  <div class="application-row__meta">
                    <app-status-pill [label]="item.application.status" />
                    <span>{{ item.interviews.length }} interview{{ item.interviews.length === 1 ? '' : 's' }}</span>
                    <a class="ghost-button" [routerLink]="['/candidate/jobs', item.application.jobId]">View role</a>
                  </div>
                </article>
              }
            </div>
          } @else {
            <app-empty-state icon="AP" title="Your pipeline is empty" description="Browse jobs and apply to start tracking company-wise progress." />
          }
        </section>

        <section class="grid-two">
          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Recommended for you</span>
                <h2>Fresh open jobs</h2>
              </div>
              <a class="ghost-button" routerLink="/candidate/jobs">See all jobs</a>
            </div>
            @if (recentJobs().length) {
              <div class="surface-list">
                @for (job of recentJobs(); track job.jobId) {
                  <article class="list-row">
                    <div class="job-market-card__top">
                      <span class="company-mark">{{ companyMark(job.companyName || job.title) }}</span>
                      <div>
                        <h3>{{ job.title }}</h3>
                        <p>{{ job.companyName || 'Company not added' }} | {{ job.location }}</p>
                      </div>
                    </div>
                    <a class="ghost-button" [routerLink]="['/candidate/jobs', job.jobId]">View</a>
                  </article>
                }
              </div>
            } @else {
              <app-empty-state icon="JB" title="No open jobs" description="Open roles will appear here when recruiters publish them." />
            }
          </article>

          <article class="workspace-panel">
            <div class="page-header">
              <div>
                <span class="eyebrow">Next step</span>
                <h2>{{ nextStepTitle() }}</h2>
                <p>{{ nextStepDescription() }}</p>
              </div>
              <a class="primary-button" [routerLink]="nextStepLink()">Continue</a>
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
  protected readonly stats = signal({
    jobs: 0,
    applications: 0,
    companies: 0,
    offers: 0,
    rejections: 0,
    active: 0,
    interviews: 0,
    unreadNotifications: 0,
    offerRate: 0,
  });
  protected readonly recentJobs = signal<JobResponse[]>([]);
  protected readonly pipeline = signal<PipelineItem[]>([]);
  protected readonly statusBreakdown = signal<{ label: string; count: number }[]>([]);
  protected readonly companySummaries = signal<{ name: string; total: number; offers: number; rejections: number }[]>([]);
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
              return of({
                ...result,
                pipeline: [] as PipelineItem[],
                unreadNotifications: result.activity.filter((item) => !item.isRead).length,
                interviewCount: 0,
              });
            }
            return forkJoin(
              result.applications.map((application) =>
                forkJoin({
                  job: this.jobs.getJob(application.jobId).pipe(catchError(() => of(null))),
                  interviews: this.interviews.getByApplication(application.applicationId).pipe(catchError(() => of([] as InterviewResponse[]))),
                }).pipe(
                  switchMap(({ job, interviews }) => of({ application, job, interviews })),
                ),
              ),
            ).pipe(
              switchMap((pipeline) =>
                of({
                  ...result,
                  pipeline,
                  unreadNotifications: result.activity.filter((item) => !item.isRead).length,
                  interviewCount: pipeline.flatMap((item) => item.interviews).length,
                }),
              ),
            );
          }),
        );
      }),
    ).subscribe((result: CandidateDashboardData) => {
      this.recentJobs.set((result.jobs ?? []).slice(0, 4));
      this.pipeline.set(result.pipeline ?? []);
      this.statusBreakdown.set(this.buildStatusBreakdown(result.pipeline ?? []));
      this.companySummaries.set(this.buildCompanySummaries(result.pipeline ?? []));
      const applications = result.pipeline ?? [];
      const offers = applications.filter((item) => this.isOfferStatus(item.application.status)).length;
      const rejections = applications.filter((item) => item.application.status === 'REJECTED').length;
      const active = applications.filter((item) => this.isActiveStatus(item.application.status)).length;
      const companyCount = new Set(applications.map((item) => this.companyName(item))).size;
      this.stats.set({
        jobs: (result.jobs ?? []).length,
        applications: applications.length,
        companies: companyCount,
        offers,
        rejections,
        active,
        interviews: typeof result.interviewCount === 'number' ? result.interviewCount : 0,
        unreadNotifications: result.unreadNotifications ?? 0,
        offerRate: applications.length ? Math.round((offers / applications.length) * 100) : 0,
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

  protected companyName(item: PipelineItem): string {
    return item.job?.companyName || `Company ${item.job?.postedBy ?? item.application.jobId}`;
  }

  protected nextStepTitle(): string {
    if (this.stats().offers > 0) {
      return 'Review your offers';
    }
    if (this.stats().interviews > 0) {
      return 'Prepare for interviews';
    }
    if (this.stats().applications > 0) {
      return 'Keep applying strategically';
    }
    return 'Start your application pipeline';
  }

  protected nextStepDescription(): string {
    if (this.stats().offers > 0) {
      return 'Open your applications to accept or decline offers from recruiters.';
    }
    if (this.stats().interviews > 0) {
      return 'Confirm slots, join online rooms, and manage reschedule requests.';
    }
    if (this.stats().applications > 0) {
      return 'Track decisions while adding more well-matched companies to your list.';
    }
    return 'Browse jobs and apply so this dashboard can track companies, interviews, offers, and rejections.';
  }

  protected nextStepLink(): string {
    if (this.stats().interviews > 0) {
      return '/candidate/interviews';
    }
    if (this.stats().applications > 0 || this.stats().offers > 0) {
      return '/candidate/applications';
    }
    return '/candidate/jobs';
  }

  private buildStatusBreakdown(pipeline: PipelineItem[]): { label: string; count: number }[] {
    const statuses = ['APPLIED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'OFFERED', 'REJECTED', 'OFFER_ACCEPTED', 'OFFER_DECLINED'];
    return statuses
      .map((status) => ({
        label: this.formatStatus(status),
        count: pipeline.filter((item) => item.application.status === status).length,
      }))
      .filter((item) => item.count > 0);
  }

  private buildCompanySummaries(pipeline: PipelineItem[]): { name: string; total: number; offers: number; rejections: number }[] {
    const summaries = new Map<string, { name: string; total: number; offers: number; rejections: number }>();
    pipeline.forEach((item) => {
      const name = this.companyName(item);
      const summary = summaries.get(name) ?? { name, total: 0, offers: 0, rejections: 0 };
      summary.total += 1;
      summary.offers += this.isOfferStatus(item.application.status) ? 1 : 0;
      summary.rejections += item.application.status === 'REJECTED' ? 1 : 0;
      summaries.set(name, summary);
    });
    return Array.from(summaries.values()).sort((a, b) => b.total - a.total).slice(0, 5);
  }

  private isOfferStatus(status: string): boolean {
    return ['OFFERED', 'OFFER_ACCEPTED', 'OFFER_DECLINED'].includes(status);
  }

  private isActiveStatus(status: string): boolean {
    return ['APPLIED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'OFFERED'].includes(status);
  }

  private formatStatus(status: string): string {
    return status
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
