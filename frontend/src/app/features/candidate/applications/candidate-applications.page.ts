import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';

import { ApplicationResponse } from '../../../core/models/application.models';
import { JobResponse } from '../../../core/models/job.models';
import { ApplicationService } from '../../../core/services/application.service';
import { JobService } from '../../../core/services/job.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface CandidateApplicationView {
  application: ApplicationResponse;
  job: JobResponse | null;
}

@Component({
  selector: 'app-candidate-applications-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatCardComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Applications"
        title="Track every role you have submitted"
        description="Withdraw where needed and monitor how far each application has moved in the hiring pipeline."
      />

      @if (applications().length) {
        <section class="stats-grid">
          <app-stat-card icon="AP" label="Applied" [value]="applications().length" caption="Applications submitted" />
          <app-stat-card icon="AN" label="Under Review" [value]="countByStatus('SHORTLISTED')" caption="Actively being reviewed" />
          <app-stat-card icon="IV" label="Interview" [value]="countByStatus('INTERVIEW_SCHEDULED')" caption="Interviews scheduled" />
          <app-stat-card icon="SL" label="Offer" [value]="countByStatus('OFFERED')" caption="Offers received" />
          <app-stat-card icon="NT" label="Withdrawn" [value]="countByStatus('WITHDRAWN')" caption="Applications withdrawn" />
        </section>

        <div class="surface-list">
          @for (item of applications(); track item.application.applicationId) {
            <article class="workspace-panel">
              <div class="notification-row">
                <div class="job-market-card__top">
                  <span class="company-mark">{{ companyMark(item.job?.title ?? 'HC') }}</span>
                  <div>
                    <span class="eyebrow">Application #{{ item.application.applicationId }}</span>
                    <h2>{{ item.job?.title ?? 'Unknown job' }}</h2>
                    <p>{{ item.job?.location ?? 'Unavailable' }} | Applied on {{ item.application.appliedAt | date:'mediumDate' }}</p>
                  </div>
                </div>
                <app-status-pill [label]="item.application.status" />
              </div>
              @if (item.application.coverLetter) {
                <p class="muted">{{ item.application.coverLetter }}</p>
              }
              @if (item.application.status === 'OFFERED') {
                <section class="offer-response-panel">
                  <div>
                    <span class="eyebrow">Offer letter</span>
                    <h3>Recruiter has offered you this role</h3>
                    <p>Review the opportunity and respond to the offer from your candidate workspace.</p>
                  </div>
                  <div class="button-row">
                    <button class="primary-button" type="button" (click)="acceptOffer(item.application.applicationId)">Accept offer</button>
                    <button class="ghost-button" type="button" (click)="declineOffer(item.application.applicationId)">Decline offer</button>
                  </div>
                </section>
              }
              @if (item.application.status === 'OFFER_ACCEPTED') {
                <section class="offer-response-panel offer-response-panel--accepted">
                  <span class="material-symbols-rounded">verified</span>
                  <div>
                    <h3>Offer accepted</h3>
                    <p>You accepted this offer. The recruiter can now continue with onboarding.</p>
                  </div>
                </section>
              }
              @if (item.application.status === 'OFFER_DECLINED') {
                <section class="offer-response-panel offer-response-panel--declined">
                  <span class="material-symbols-rounded">do_not_disturb_on</span>
                  <div>
                    <h3>Offer declined</h3>
                    <p>You declined this offer. The application is now closed from your side.</p>
                  </div>
                </section>
              }
              <div class="button-row">
                @if (item.job) {
                  <a class="ghost-button" [routerLink]="['/candidate/jobs', item.job.jobId]">View job</a>
                }
                @if (canWithdraw(item.application.status)) {
                  <button class="danger-button" type="button" (click)="withdraw(item.application.applicationId)">Withdraw</button>
                }
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="AP" title="No applications yet" description="Once you apply for roles, your history and status updates will appear here." actionLabel="Browse jobs" actionLink="/candidate/jobs" />
      }
    </section>
  `,
})
export class CandidateApplicationsPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly applicationsService = inject(ApplicationService);
  private readonly jobs = inject(JobService);
  private readonly toast = inject(ToastService);

  protected readonly applications = signal<CandidateApplicationView[]>([]);

  constructor() {
    this.load();
  }

  protected canWithdraw(status: string): boolean {
    return !['WITHDRAWN', 'REJECTED', 'OFFERED', 'OFFER_ACCEPTED', 'OFFER_DECLINED'].includes(status);
  }

  protected countByStatus(status: string): number {
    return this.applications().filter((item) => item.application.status === status).length;
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

  protected withdraw(applicationId: number): void {
    this.applicationsService.withdraw(applicationId).subscribe(() => {
      this.toast.success('Application withdrawn', 'The application is marked as withdrawn.');
      this.load();
    });
  }

  protected acceptOffer(applicationId: number): void {
    this.applicationsService.acceptOffer(applicationId).subscribe(() => {
      this.toast.success('Offer accepted', 'Your response has been saved.');
      this.load();
    });
  }

  protected declineOffer(applicationId: number): void {
    this.applicationsService.declineOffer(applicationId).subscribe(() => {
      this.toast.info('Offer declined', 'Your response has been saved.');
      this.load();
    });
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().pipe(
      switchMap((profile) => {
        if (!profile) {
          return of([] as CandidateApplicationView[]);
        }
        return this.applicationsService.getByCandidate(profile.profileId).pipe(
          switchMap((applications) => {
            if (!applications.length) {
              return of([] as CandidateApplicationView[]);
            }
            const uniqueJobIds = [...new Set(applications.map((application) => application.jobId))];
            return forkJoin(uniqueJobIds.map((jobId) => this.jobs.getJob(jobId))).pipe(
              switchMap((jobs) => {
                const jobsById = new Map<number, JobResponse>(jobs.map((job) => [job.jobId, job]));
                return of(
                  applications.map((application) => ({
                    application,
                    job: jobsById.get(application.jobId) ?? null,
                  })),
                );
              }),
            );
          }),
        );
      }),
    ).subscribe((applications) => this.applications.set(applications));
  }
}
