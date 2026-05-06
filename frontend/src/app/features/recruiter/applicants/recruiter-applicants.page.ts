import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { ApplicationResponse } from '../../../core/models/application.models';
import { InterviewResponse } from '../../../core/models/interview.models';
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
  interviews: InterviewResponse[];
}

type ScheduleForm = FormGroup;

@Component({
  selector: 'app-recruiter-applicants-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
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

              <section class="applicant-profile-panel">
                <div class="applicant-profile-panel__head">
                  <span class="material-symbols-rounded">account_box</span>
                  <div>
                    <h3>Candidate profile</h3>
                    <p>Read-only profile details submitted by the applicant.</p>
                  </div>
                </div>

                @if (item.candidate) {
                  <div class="applicant-profile-grid">
                    <div>
                      <label>Full name</label>
                      <strong>{{ item.candidate.fullName }}</strong>
                    </div>
                    <div>
                      <label>Email</label>
                      <strong>{{ item.candidate.email }}</strong>
                    </div>
                    <div>
                      <label>Mobile</label>
                      <strong>{{ item.candidate.mobile || 'Not added' }}</strong>
                    </div>
                    <div>
                      <label>Experience</label>
                      <strong>{{ item.candidate.experience || 0 }} years</strong>
                    </div>
                    <div>
                      <label>Gender</label>
                      <strong>{{ formatLabel(item.candidate.gender) }}</strong>
                    </div>
                    <div>
                      <label>Date of birth</label>
                      <strong>{{ item.candidate.dob ? (item.candidate.dob | date:'mediumDate') : 'Not added' }}</strong>
                    </div>
                    <div class="applicant-profile-grid__wide">
                      <label>Location</label>
                      <strong>{{ locationSummary(item.candidate) }}</strong>
                    </div>
                    <div class="applicant-profile-grid__wide">
                      <label>Skills</label>
                      <div class="chip-group">
                        @for (skill of item.candidate.skills; track skill) {
                          <span class="chip">{{ skill }}</span>
                        } @empty {
                          <span class="chip">No skills added</span>
                        }
                      </div>
                    </div>
                  </div>
                } @else {
                  <p class="muted">Candidate profile could not be loaded right now.</p>
                }

                <div class="button-row">
                  @if (resumeUrl(item)) {
                    <a class="primary-button" [href]="resumeUrl(item)" target="_blank" rel="noreferrer">
                      <span class="material-symbols-rounded">description</span>
                      View resume
                    </a>
                  } @else {
                    <button class="ghost-button" type="button" disabled>No resume available</button>
                  }
                </div>
              </section>

              @if (item.interviews.length) {
                <section class="interview-summary-panel">
                  <div class="applicant-profile-panel__head">
                    <span class="material-symbols-rounded">video_camera_front</span>
                    <div>
                      <h3>Interview slots</h3>
                      <p>Scheduled interview rooms for this applicant.</p>
                    </div>
                  </div>
                  <div class="interview-summary-list">
                    @for (interview of item.interviews; track interview.interviewId) {
                      <article>
                        <div>
                          <span class="eyebrow">Interview {{ interview.interviewId }}</span>
                          <strong>{{ interview.mode }} interview</strong>
                          <p>{{ interview.scheduledAt | date:'medium' }}</p>
                          <small>{{ interview.location || interview.meetLink || 'Details pending' }}</small>
                        </div>
                        <app-status-pill [label]="interview.status" />
                        <div class="button-row">
                          @if (interview.status === 'RESCHEDULE_REQUESTED') {
                            <button class="primary-button" type="button" (click)="acceptReschedule(interview.interviewId)">Accept request</button>
                            <button class="ghost-button" type="button" (click)="declineReschedule(interview.interviewId)">Decline</button>
                          }
                          @if (isOnlineInterview(interview)) {
                            <a class="primary-button" [routerLink]="['/recruiter/interviews', interview.interviewId, 'join']">Join room</a>
                          } @else {
                            <span class="muted">In-person interview</span>
                          }
                        </div>
                      </article>
                      @if (interview.status === 'RESCHEDULE_REQUESTED') {
                        <article class="reschedule-request-card">
                          <span class="material-symbols-rounded">event_repeat</span>
                          <div>
                            <strong>Candidate requested a new slot</strong>
                            <p>Requested time: {{ interview.requestedScheduledAt ? (interview.requestedScheduledAt | date:'medium') : 'Not provided' }}</p>
                            @if (interview.requestedMeetLink) {
                              <p>Requested meet link: {{ interview.requestedMeetLink }}</p>
                            }
                            @if (interview.requestedLocation) {
                              <p>Requested location: {{ interview.requestedLocation }}</p>
                            }
                            @if (interview.requestedNotes) {
                              <p>Reason: {{ interview.requestedNotes }}</p>
                            }
                          </div>
                        </article>
                      }
                    }
                  </div>
                </section>
              }

              <div class="button-row">
                @if (canMoveTo(item.application.status, 'SHORTLISTED')) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'SHORTLISTED')">Shortlist</button>
                }
                @if (canMoveTo(item.application.status, 'REJECTED') && canRejectNow(item)) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'REJECTED')">Reject</button>
                }
                @if (canMoveTo(item.application.status, 'OFFERED') && interviewReadyForDecision(item)) {
                  <button class="ghost-button" type="button" (click)="updateStatus(item.application.applicationId, 'OFFERED')">Offer</button>
                }
                @if (item.application.status === 'INTERVIEW_SCHEDULED' && !interviewReadyForDecision(item)) {
                  <span class="muted">Offer/reject unlocks after candidate confirmation.</span>
                }
              </div>

              @if (canScheduleInterview(item)) {
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
                    <h3>{{ item.interviews.length ? 'Interview already scheduled' : 'Interview scheduling locked' }}</h3>
                    <p>{{ item.interviews.length ? 'Use the interview slots panel above to review the scheduled slot.' : 'Shortlist this application before scheduling an interview.' }}</p>
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

  protected canScheduleInterview(item: ApplicantView): boolean {
    return item.application.status === 'SHORTLISTED' && item.interviews.length === 0;
  }

  protected acceptReschedule(interviewId: number): void {
    this.interviews.acceptReschedule(interviewId).subscribe({
      next: () => {
        this.toast.success('Reschedule accepted', 'The interview was moved to the candidate requested slot.');
        this.load();
      },
      error: (error: unknown) => this.toast.error('Reschedule update failed', getErrorMessage(error, 'Unable to accept this request.')),
    });
  }

  protected declineReschedule(interviewId: number): void {
    this.interviews.declineReschedule(interviewId).subscribe({
      next: () => {
        this.toast.success('Reschedule declined', 'The original recruiter scheduled interview slot remains active.');
        this.load();
      },
      error: (error: unknown) => this.toast.error('Reschedule update failed', getErrorMessage(error, 'Unable to decline this request.')),
    });
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
        return forkJoin(applications.map((application) =>
          forkJoin({
            candidate: this.profiles.getProfileById(application.candidateId).pipe(catchError(() => of(null))),
            interviews: this.interviews.getByApplication(application.applicationId).pipe(catchError(() => of([] as InterviewResponse[]))),
          }).pipe(
            map(({ candidate, interviews }) => ({
              application,
              candidate,
              interviews,
            })),
          ),
        )).pipe(
          map((items) => items as ApplicantView[]),
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

  protected resumeUrl(item: ApplicantView): string | null {
    return item.application.resumeUrl || item.candidate?.resumeUrl || null;
  }

  protected interviewReadyForDecision(item: ApplicantView): boolean {
    return item.interviews.some((interview) =>
      interview.status === 'CONFIRMED'
    );
  }

  protected canRejectNow(item: ApplicantView): boolean {
    return item.application.status !== 'INTERVIEW_SCHEDULED' || this.interviewReadyForDecision(item);
  }

  protected isOnlineInterview(interview: InterviewResponse): boolean {
    return interview.mode === 'ONLINE' && !!interview.meetLink;
  }

  protected locationSummary(candidate: ProfileResponse): string {
    const address = candidate.addresses?.[0];
    if (!address) {
      return 'Not added';
    }
    return [address.houseNo, address.street, address.city, address.state, address.pincode]
      .filter(Boolean)
      .join(', ');
  }

  protected formatLabel(value: string | null): string {
    if (!value) {
      return 'Not added';
    }
    return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
  }
}
