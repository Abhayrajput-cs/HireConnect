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
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface CandidateApplicationView {
  application: ApplicationResponse;
  job: JobResponse | null;
}

@Component({
  selector: 'app-candidate-applications-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Applications"
        title="Track every role you have submitted"
        description="Withdraw where needed and monitor how far each application has moved in the hiring pipeline."
      />

      @if (applications().length) {
        <div class="surface-list">
          @for (item of applications(); track item.application.applicationId) {
            <article class="card-shell content-card">
              <div class="page-header">
                <div>
                  <span class="eyebrow">Application #{{ item.application.applicationId }}</span>
                  <h2>{{ item.job?.title ?? 'Unknown job' }}</h2>
                  <p>{{ item.job?.location ?? 'Unavailable' }} | Applied on {{ item.application.appliedAt | date:'mediumDate' }}</p>
                </div>
                <app-status-pill [label]="item.application.status" />
              </div>
              @if (item.application.coverLetter) {
                <p class="muted">{{ item.application.coverLetter }}</p>
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
    return !['WITHDRAWN', 'REJECTED', 'OFFERED'].includes(status);
  }

  protected withdraw(applicationId: number): void {
    this.applicationsService.withdraw(applicationId).subscribe(() => {
      this.toast.success('Application withdrawn', 'The application is marked as withdrawn.');
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
