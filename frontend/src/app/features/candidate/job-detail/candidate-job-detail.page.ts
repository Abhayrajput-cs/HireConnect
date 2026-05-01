import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ApplicationResponse } from '../../../core/models/application.models';
import { JobResponse } from '../../../core/models/job.models';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { ApplicationService } from '../../../core/services/application.service';
import { BookmarksService } from '../../../core/services/bookmarks.service';
import { JobService } from '../../../core/services/job.service';
import { SessionService } from '../../../core/services/session.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-candidate-job-detail-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, PageHeaderComponent, EmptyStateComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      @if (job()) {
        <app-page-header
          eyebrow="Job detail"
          [title]="job()!.title"
          [description]="(job()!.companyName || 'Company not added') + ' | ' + job()!.category + ' | ' + job()!.location + ' | ' + job()!.type"
          actionLabel="Back to jobs"
          actionLink="/candidate/jobs"
        />

        <section class="grid-two">
          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">{{ job()!.category }}</span>
                <h2>{{ job()!.title }}</h2>
                <p>{{ job()!.companyName || 'Company not added' }}</p>
              </div>
              <app-status-pill [label]="job()!.status" />
            </div>
            <p>{{ job()!.description }}</p>

            <div class="chip-group">
              @for (skill of job()!.skills; track skill) {
                <span class="chip">{{ skill }}</span>
              }
            </div>

            <div class="meta-list">
              <div><span>Location</span><strong>{{ job()!.location }}</strong></div>
              <div><span>Salary</span><strong>{{ job()!.salaryMin | currency:'INR':'symbol':'1.0-0' }} - {{ job()!.salaryMax | currency:'INR':'symbol':'1.0-0' }}</strong></div>
              <div><span>Experience required</span><strong>{{ job()!.experienceRequired }} years</strong></div>
              <div><span>Posted on</span><strong>{{ job()!.postedAt | date:'mediumDate' }}</strong></div>
            </div>

            @if (job()!.status === 'CLOSED') {
              <div class="job-state-banner job-state-banner--closed">
                <span class="material-symbols-rounded">lock</span>
                <div>
                  <strong>Hiring closed</strong>
                  <p>This role is still visible for transparency, but new applications are disabled.</p>
                </div>
              </div>
            }
          </article>

          <article class="card-shell content-card">
            <div class="page-header">
              <div>
                <span class="eyebrow">Apply workflow</span>
                <h2>Submit your application</h2>
                <p>Your application request maps directly to application-service.</p>
              </div>
            </div>

            @if (!profileId()) {
              <app-empty-state
                icon="PF"
                title="Candidate profile required"
                description="Create your candidate profile first so the backend can link your application correctly."
                actionLabel="Open profile"
                actionLink="/candidate/profile"
              />
            } @else if (job()!.status === 'CLOSED') {
              <app-empty-state
                icon="JB"
                title="Applications closed"
                description="Recruiter hiring is closed for this role, so you can view it but cannot apply."
              />
            } @else if (existingApplication()) {
              <app-empty-state
                icon="AP"
                title="Already applied"
                description="You have already submitted an application for this role."
                actionLabel="View applications"
                actionLink="/candidate/applications"
              />
            } @else {
              <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
                <div class="field-block">
                  <label for="resumeUrl">Resume URL</label>
                  <input id="resumeUrl" formControlName="resumeUrl" />
                </div>
                <div class="field-block">
                  <label for="coverLetter">Cover letter</label>
                  <textarea id="coverLetter" rows="5" formControlName="coverLetter" placeholder="A concise note on why you are a fit"></textarea>
                </div>
                @if (errorMessage()) {
                  <small>{{ errorMessage() }}</small>
                }
                <div class="button-row">
                  <button class="primary-button" type="submit" [disabled]="form.invalid || applying()">Apply now</button>
                  <button class="ghost-button" type="button" (click)="bookmarks.toggle(job()!.jobId)">
                    {{ bookmarks.isBookmarked(job()!.jobId) ? 'Remove bookmark' : 'Save job' }}
                  </button>
                </div>
              </form>
            }
          </article>
        </section>
      } @else {
        <app-empty-state icon="JB" title="Job not found" description="This role could not be loaded from the backend." actionLabel="Back to jobs" actionLink="/candidate/jobs" />
      }
    </section>
  `,
})
export class CandidateJobDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly jobs = inject(JobService);
  private readonly applications = inject(ApplicationService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly analytics = inject(AnalyticsService);
  protected readonly bookmarks = inject(BookmarksService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly job = signal<JobResponse | null>(null);
  protected readonly profileId = signal<number | null>(null);
  protected readonly existingApplication = signal<ApplicationResponse | null>(null);
  protected readonly applying = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly form = this.fb.nonNullable.group({
    resumeUrl: ['', [Validators.required]],
    coverLetter: [''],
  });

  constructor() {
    const jobId = Number(this.route.snapshot.paramMap.get('jobId'));
    if (!Number.isFinite(jobId)) {
      return;
    }

    this.jobs.getJob(jobId).subscribe((job) => {
      this.job.set(job);
      void this.analytics.recordJobView(job.jobId, { viewerId: this.session.user()?.userId }).subscribe();
    });

    this.viewerProfile.getCurrentProfile().subscribe((profile) => {
      if (!profile) {
        return;
      }
      this.profileId.set(profile.profileId);
      this.form.controls.resumeUrl.setValue(profile.resumeUrl ?? '');
      this.applications.getByCandidate(profile.profileId).subscribe((applications) => {
        this.existingApplication.set(applications.find((application) => application.jobId === jobId) ?? null);
      });
    });
  }

  protected submit(): void {
    if (!this.job() || !this.profileId() || this.job()!.status === 'CLOSED' || this.form.invalid || this.applying()) {
      this.form.markAllAsTouched();
      if (this.job()?.status === 'CLOSED') {
        this.errorMessage.set('Applications are closed for this job.');
      }
      return;
    }

    this.applying.set(true);
    this.errorMessage.set('');
    this.applications.submit({
      jobId: this.job()!.jobId,
      candidateId: this.profileId()!,
      resumeUrl: this.form.controls.resumeUrl.getRawValue(),
      coverLetter: this.form.controls.coverLetter.getRawValue().trim() || null,
    }).subscribe({
      next: (application) => {
        this.existingApplication.set(application);
        this.toast.success('Application submitted', 'Your application is now in the hiring pipeline.');
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to submit the application.'));
        this.toast.error('Application failed', this.errorMessage());
      },
      complete: () => this.applying.set(false),
    });
  }
}
