import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';

import { ApplicationResponse } from '../../../core/models/application.models';
import { ProfileResponse } from '../../../core/models/profile.models';
import { ApplicationService } from '../../../core/services/application.service';
import { InterviewService } from '../../../core/services/interview.service';
import { JobService } from '../../../core/services/job.service';
import { ProfileService } from '../../../core/services/profile.service';
import { ToastService } from '../../../core/services/toast.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface ApplicantView {
  application: ApplicationResponse;
  candidate: ProfileResponse | null;
}

type ScheduleForm = FormGroup;

@Component({
  selector: 'app-recruiter-applicants-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Applicants"
        [title]="jobTitle()"
        description="Review applicants, update status, and schedule interviews from one recruiter workspace."
      />

      @if (applicants().length) {
        <div class="surface-list">
          @for (item of applicants(); track item.application.applicationId) {
            <article class="card-shell content-card">
              <div class="page-header">
                <div>
                  <span class="eyebrow">Application {{ item.application.applicationId }}</span>
                  <h2>{{ item.candidate?.fullName || item.candidate?.email || 'Candidate' }}</h2>
                  <p>{{ item.candidate?.email || 'Email unavailable' }}</p>
                </div>
                <app-status-pill [label]="item.application.status" />
              </div>
              <p class="muted">{{ item.application.coverLetter || 'No cover letter submitted.' }}</p>
              <div class="button-row">
                @if (canMoveTo(item.application.status, 'SHORTLISTED')) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'SHORTLISTED')">Shortlist</button>
                }
                @if (canMoveTo(item.application.status, 'REJECTED')) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'REJECTED')">Reject</button>
                }
                @if (canMoveTo(item.application.status, 'OFFERED')) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'OFFERED')">Offer</button>
                }
              </div>

              @if (canScheduleInterview(item.application.status)) {
                <form class="stack" [formGroup]="scheduleForms()[item.application.applicationId]" (ngSubmit)="schedule(item.application.applicationId)">
                  <div class="form-grid form-grid--wide">
                    <div class="field-block">
                      <label>Schedule</label>
                      <input type="datetime-local" formControlName="scheduledAt" />
                    </div>
                    <div class="field-block">
                      <label>Mode</label>
                      <select formControlName="mode" (change)="syncModeValidation(item.application.applicationId)">
                        <option value="ONLINE">ONLINE</option>
                        <option value="IN_PERSON">IN_PERSON</option>
                      </select>
                    </div>
                    <div class="field-block">
                      <label>Meet link</label>
                      <input formControlName="meetLink" placeholder="Required for ONLINE interviews" />
                      @if (showFieldError(item.application.applicationId, 'meetLink')) {
                        <small>Meet link is required for ONLINE interviews.</small>
                      }
                    </div>
                    <div class="field-block">
                      <label>Location</label>
                      <input formControlName="location" placeholder="Required for IN_PERSON interviews" />
                      @if (showFieldError(item.application.applicationId, 'location')) {
                        <small>Location is required for IN_PERSON interviews.</small>
                      }
                    </div>
                  </div>
                  <div class="field-block">
                    <label>Notes</label>
                    <textarea rows="3" formControlName="notes"></textarea>
                  </div>
                  @if (scheduleErrors()[item.application.applicationId]) {
                    <small>{{ scheduleErrors()[item.application.applicationId] }}</small>
                  }
                  <div class="form-actions">
                    <button class="primary-button" type="submit">Schedule interview</button>
                  </div>
                </form>
              } @else {
                <div class="surface-list">
                  <article>
                    <h3>Interview scheduling locked</h3>
                    <p>Interviews can only be scheduled when an application is in SHORTLISTED or INTERVIEW_SCHEDULED state.</p>
                  </article>
                </div>
              }
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="AP" title="No applicants yet" description="As candidates apply to this job, they will appear here." />
      }
    </section>
  `,
})
export class RecruiterApplicantsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly applications = inject(ApplicationService);
  private readonly profiles = inject(ProfileService);
  private readonly interviews = inject(InterviewService);
  private readonly jobs = inject(JobService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly applicants = signal<ApplicantView[]>([]);
  protected readonly scheduleForms = signal<Record<number, ScheduleForm>>({});
  protected readonly scheduleErrors = signal<Record<number, string>>({});
  protected readonly jobTitle = signal('Applicant review');

  private readonly jobId = Number(this.route.snapshot.paramMap.get('jobId'));

  constructor() {
    this.load();
  }

  protected updateStatus(applicationId: number, status: string): void {
    this.applications.updateStatus(applicationId, { status }).subscribe({
      next: () => {
        this.toast.success('Application updated', `Status changed to ${status}.`);
        this.load();
      },
      error: (error: unknown) => {
        this.toast.error('Status update blocked', getErrorMessage(error, 'That application status change is not allowed.'));
      },
    });
  }

  protected schedule(applicationId: number): void {
    const form = this.scheduleForms()[applicationId];
    if (!form) {
      return;
    }
    form.markAllAsTouched();
    this.syncModeValidation(applicationId);
    if (form.invalid) {
      this.scheduleErrors.update((errors) => ({
        ...errors,
        [applicationId]: 'Complete the required interview fields before scheduling.',
      }));
      return;
    }
    const raw = form.getRawValue() as {
      scheduledAt: string;
      mode: string;
      meetLink: string;
      location: string;
      notes: string;
    };
    this.interviews.schedule({
      applicationId,
      scheduledAt: raw.scheduledAt,
      mode: raw.mode,
      meetLink: raw.meetLink.trim() || null,
      location: raw.location.trim() || null,
      notes: raw.notes.trim() || null,
    }).subscribe({
      next: () => {
        this.scheduleErrors.update((errors) => {
          const next = { ...errors };
          delete next[applicationId];
          return next;
        });
        this.toast.success('Interview scheduled', 'The candidate can now see the interview event.');
        this.load();
      },
      error: (error: unknown) => {
        this.scheduleErrors.update((errors) => ({
          ...errors,
          [applicationId]: getErrorMessage(error, 'Interview scheduling failed.'),
        }));
        this.toast.error('Interview scheduling failed', getErrorMessage(error, 'Please check the interview fields and application status.'));
      },
    });
  }

  protected canMoveTo(currentStatus: string, targetStatus: string): boolean {
    return this.allowedTransitions(currentStatus).includes(targetStatus);
  }

  protected canScheduleInterview(status: string): boolean {
    return status === 'SHORTLISTED' || status === 'INTERVIEW_SCHEDULED';
  }

  protected showFieldError(applicationId: number, fieldName: 'meetLink' | 'location'): boolean {
    const control = this.scheduleForms()[applicationId]?.get(fieldName);
    return !!control && control.invalid && control.touched;
  }

  protected syncModeValidation(applicationId: number): void {
    const form = this.scheduleForms()[applicationId];
    this.applyModeValidation(form);
  }

  private applyModeValidation(form: ScheduleForm | undefined): void {
    if (!form) {
      return;
    }

    const mode = form.get('mode')?.value;
    const meetLink = form.get('meetLink');
    const location = form.get('location');

    if (!meetLink || !location) {
      return;
    }

    if (mode === 'ONLINE') {
      meetLink.setValidators([Validators.required]);
      location.clearValidators();
    } else {
      location.setValidators([Validators.required]);
      meetLink.clearValidators();
    }

    meetLink.updateValueAndValidity({ emitEvent: false });
    location.updateValueAndValidity({ emitEvent: false });
  }

  private load(): void {
    if (!Number.isFinite(this.jobId)) {
      return;
    }

    this.jobs.getJob(this.jobId).subscribe((job) => this.jobTitle.set(`${job.title} applicants`));
    this.applications.getByJob(this.jobId).pipe(
      switchMap((applications) => {
        if (!applications.length) {
          return of([] as ApplicantView[]);
        }
        const uniqueCandidateIds = [...new Set(applications.map((application) => application.candidateId))];
        return forkJoin(uniqueCandidateIds.map((candidateId) => this.profiles.getProfileById(candidateId))).pipe(
          switchMap((profiles) => {
            const profilesById = new Map<number, ProfileResponse>(profiles.map((profile) => [profile.profileId, profile]));
            return of(applications.map((application) => ({
              application,
              candidate: profilesById.get(application.candidateId) ?? null,
            })));
          }),
        );
      }),
    ).subscribe((applicants) => {
      this.applicants.set(applicants);
      const forms: Record<number, ScheduleForm> = {};
      applicants.forEach((item) => {
        forms[item.application.applicationId] = this.fb.nonNullable.group({
          scheduledAt: ['', [Validators.required]],
          mode: ['ONLINE'],
          meetLink: [''],
          location: [''],
          notes: [''],
        });
        this.applyModeValidation(forms[item.application.applicationId]);
      });
      this.scheduleForms.set(forms);
      this.scheduleErrors.set({});
    });
  }

  private allowedTransitions(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'APPLIED':
        return ['SHORTLISTED', 'REJECTED'];
      case 'SHORTLISTED':
        return ['REJECTED'];
      case 'INTERVIEW_SCHEDULED':
        return ['OFFERED', 'REJECTED'];
      default:
        return [];
    }
  }
}
