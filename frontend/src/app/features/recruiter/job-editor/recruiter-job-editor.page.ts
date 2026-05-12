import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { JobRequest, JobResponse } from '../../../core/models/job.models';
import { JobService } from '../../../core/services/job.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-recruiter-job-editor-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EmptyStateComponent, PageHeaderComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Job editor"
        [title]="existingJob() ? 'Refine your posting' : 'Create a new role'"
        description="This form writes directly to the job-service create and update endpoints."
      />

      @if (!hasRecruiterProfile()) {
        <app-empty-state
          icon="PF"
          title="Recruiter profile required"
          description="Create your recruiter profile first so each new job can be linked to your company identity."
          actionLabel="Open recruiter profile"
          actionLink="/recruiter/profile"
        />
      } @else {
      <section class="card-shell content-card">
        <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-grid form-grid--wide">
            <div class="field-block">
              <label>Title</label>
              <input formControlName="title" />
            </div>
            <div class="field-block">
              <label>Category</label>
              <input formControlName="category" />
            </div>
            <div class="field-block">
              <label>Type</label>
              <input formControlName="type" placeholder="Full-time" />
            </div>
            <div class="field-block">
              <label>Location</label>
              <input formControlName="location" />
            </div>
            <div class="field-block">
              <label>Salary min</label>
              <input type="number" formControlName="salaryMin" />
            </div>
          <div class="field-block">
            <label>Salary max</label>
            <input type="number" formControlName="salaryMax" />
          </div>
            <div class="field-block">
              <label>Experience required</label>
              <input type="number" formControlName="experienceRequired" />
            </div>
            <div class="field-block">
              <label>Status</label>
              <select formControlName="status">
                <option value="OPEN">OPEN</option>
                <option value="PAUSED">PAUSED</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </div>
          </div>

          <div class="field-block">
            <label>Description</label>
            <textarea rows="6" formControlName="description"></textarea>
          </div>

          <div class="field-block">
            <label>Skills</label>
            <textarea rows="3" formControlName="skillsText" placeholder="Java, Spring Boot, MySQL"></textarea>
          </div>

          @if (form.hasError('salaryRange')) {
            <small>Salary max must be greater than or equal to salary min.</small>
          }

          @if (errorMessage()) {
            <small>{{ errorMessage() }}</small>
          }

          <div class="form-actions">
            <button class="primary-button" type="submit" [disabled]="form.invalid || saving()">
              {{ saving() ? 'Saving...' : existingJob() ? 'Update job' : 'Create job' }}
            </button>
          </div>
        </form>
      </section>
      }
    </section>
  `,
})
export class RecruiterJobEditorPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly jobs = inject(JobService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly toast = inject(ToastService);

  protected readonly existingJob = signal<JobResponse | null>(null);
  protected readonly hasRecruiterProfile = signal(false);
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal('');
  private recruiterProfileId: number | null = null;

  protected readonly form = this.fb.nonNullable.group(
    {
      title: ['', [Validators.required]],
      category: ['', [Validators.required]],
      type: ['Full-time', [Validators.required]],
      location: ['', [Validators.required]],
      salaryMin: [0, [Validators.required, Validators.min(1)]],
      salaryMax: [0, [Validators.required, Validators.min(1)]],
      description: ['', [Validators.required]],
      skillsText: ['', [Validators.required]],
      experienceRequired: [0, [Validators.required, Validators.min(0)]],
      status: ['OPEN', [Validators.required]],
    },
    { validators: RecruiterJobEditorPageComponent.salaryRangeValidator },
  );

  constructor() {
    this.viewerProfile.getCurrentProfile(true).subscribe((profile) => {
      this.recruiterProfileId = profile?.profileId ?? null;
      this.hasRecruiterProfile.set(!!profile);
    });

    const jobIdParam = this.route.snapshot.paramMap.get('jobId');
    const jobId = jobIdParam ? Number(jobIdParam) : Number.NaN;
    if (Number.isFinite(jobId) && jobId > 0) {
      this.jobs.getJob(jobId).subscribe((job) => {
        this.existingJob.set(job);
        this.form.patchValue({
          title: job.title,
          category: job.category,
          type: job.type,
          location: job.location,
          salaryMin: job.salaryMin,
          salaryMax: job.salaryMax,
          description: job.description,
          skillsText: job.skills.join(', '),
          experienceRequired: job.experienceRequired,
          status: job.status,
        });
      });
    }
  }

  protected submit(): void {
    if (!this.recruiterProfileId || this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const payload: JobRequest = {
      title: raw.title,
      category: raw.category,
      type: raw.type,
      location: raw.location,
      salaryMin: Number(raw.salaryMin),
      salaryMax: Number(raw.salaryMax),
      description: raw.description,
      skills: this.parseSkills(raw.skillsText),
      experienceRequired: Number(raw.experienceRequired),
      postedBy: this.recruiterProfileId,
      status: raw.status,
      postedAt: this.existingJob()?.postedAt ?? null,
    };

    const request$ = this.existingJob()
      ? this.jobs.updateJob(this.existingJob()!.jobId, payload)
      : this.jobs.createJob(payload);

    request$.subscribe({
      next: () => {
        this.toast.success('Job saved', 'The job posting is now live in job-service.');
        void this.router.navigate(['/recruiter/jobs']);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to save the job.'));
        this.toast.error('Job save failed', this.errorMessage());
        this.saving.set(false);
      },
      complete: () => this.saving.set(false),
    });
  }

  private static salaryRangeValidator(control: AbstractControl): ValidationErrors | null {
    const salaryMin = Number(control.get('salaryMin')?.value ?? 0);
    const salaryMax = Number(control.get('salaryMax')?.value ?? 0);
    return salaryMax >= salaryMin ? null : { salaryRange: true };
  }

  private parseSkills(value: string): string[] {
    return value
      .split(/[\n,;]+/)
      .flatMap((chunk) => {
        const trimmed = chunk.trim();
        return trimmed.length > 80 ? trimmed.split(/\s+/) : trimmed.split(/\s{2,}/);
      })
      .map((item) => item.trim())
      .map((item) => item.slice(0, 80))
      .filter(Boolean)
      .slice(0, 30);
  }
}
