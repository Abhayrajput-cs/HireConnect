import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin, of, switchMap } from 'rxjs';

import { InterviewResponse } from '../../../core/models/interview.models';
import { ApplicationService } from '../../../core/services/application.service';
import { InterviewService } from '../../../core/services/interview.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

type RescheduleForm = FormGroup;

@Component({
  selector: 'app-candidate-interviews-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Interviews"
        title="Your allocated interview slots"
        description="Confirm upcoming conversations, track schedule changes, and send reschedule requests in a cleaner card-based flow."
      />

      @if (interviews().length) {
        <div class="activity-feed">
          @for (interview of interviews(); track interview.interviewId) {
            <article class="card-shell content-card activity-card">
              <div class="page-header">
                <div class="activity-card__meta">
                  <span class="activity-card__icon material-symbols-rounded">calendar_month</span>
                  <div>
                    <span class="eyebrow">Application {{ interview.applicationId }}</span>
                    <h2>{{ interview.mode }} interview slot</h2>
                    <p>{{ interview.scheduledAt | date:'medium' }}</p>
                    <small>{{ interview.notes || 'Your schedule and mode are synced from interview-service.' }}</small>
                  </div>
                </div>
                <app-status-pill [label]="interview.status" />
              </div>

              <div class="meta-list">
                <div><span>Meet link</span><strong>{{ interview.meetLink || 'Will be shared by recruiter' }}</strong></div>
                <div><span>Location</span><strong>{{ interview.location || 'Online / recruiter to confirm' }}</strong></div>
              </div>

              <div class="button-row">
                @if (interview.status === 'SCHEDULED') {
                  <button class="primary-button" type="button" (click)="confirm(interview.interviewId)">Confirm</button>
                }
                <button class="ghost-button" type="button" (click)="toggleReschedule(interview.interviewId)">
                  {{ editingInterviewId() === interview.interviewId ? 'Hide reschedule form' : 'Request reschedule' }}
                </button>
              </div>

              @if (editingInterviewId() === interview.interviewId) {
                <form class="stack" [formGroup]="rescheduleForms()[interview.interviewId]" (ngSubmit)="reschedule(interview.interviewId)">
                  <div class="form-grid">
                    <div class="field-block">
                      <label>Preferred new slot</label>
                      <input type="datetime-local" formControlName="scheduledAt" />
                    </div>
                    <div class="field-block">
                      <label>Meet link</label>
                      <input formControlName="meetLink" />
                    </div>
                    <div class="field-block">
                      <label>Location</label>
                      <input formControlName="location" />
                    </div>
                  </div>
                  <div class="field-block">
                    <label>Notes for recruiter</label>
                    <textarea rows="3" formControlName="notes"></textarea>
                  </div>
                  <div class="form-actions">
                    <button class="ghost-button" type="submit">Send reschedule request</button>
                  </div>
                </form>
              }
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="IV" title="No interviews scheduled" description="Once a recruiter schedules an interview, it will appear here." />
      }
    </section>
  `,
})
export class CandidateInterviewsPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly applications = inject(ApplicationService);
  private readonly interviewsService = inject(InterviewService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly interviews = signal<InterviewResponse[]>([]);
  protected readonly rescheduleForms = signal<Record<number, RescheduleForm>>({});
  protected readonly editingInterviewId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected confirm(interviewId: number): void {
    this.interviewsService.confirm(interviewId).subscribe(() => {
      this.toast.success('Interview confirmed', 'The recruiter can now see your confirmation.');
      this.load();
    });
  }

  protected reschedule(interviewId: number): void {
    const form = this.rescheduleForms()[interviewId];
    if (!form) {
      return;
    }
    const raw = form.getRawValue() as { scheduledAt: string; meetLink: string; location: string; notes: string };
    this.interviewsService.reschedule(interviewId, {
      scheduledAt: raw.scheduledAt,
      meetLink: raw.meetLink.trim() || null,
      location: raw.location.trim() || null,
      notes: raw.notes.trim() || null,
    }).subscribe(() => {
      this.toast.success('Reschedule requested', 'The updated interview request has been sent.');
      this.editingInterviewId.set(null);
      this.load();
    });
  }

  protected toggleReschedule(interviewId: number): void {
    this.editingInterviewId.set(this.editingInterviewId() === interviewId ? null : interviewId);
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile().pipe(
      switchMap((profile) => {
        if (!profile) {
          return of([] as InterviewResponse[]);
        }
        return this.applications.getByCandidate(profile.profileId).pipe(
          switchMap((applications) => {
            if (!applications.length) {
              return of([] as InterviewResponse[]);
            }
            return forkJoin(applications.map((application) => this.interviewsService.getByApplication(application.applicationId))).pipe(
              switchMap((items) => of(items.flat())),
            );
          }),
        );
      }),
    ).subscribe((interviews) => {
      this.interviews.set(interviews);
      const forms: Record<number, RescheduleForm> = {};
      interviews.forEach((interview) => {
        forms[interview.interviewId] = this.fb.nonNullable.group({
          scheduledAt: [interview.scheduledAt.slice(0, 16)],
          meetLink: [interview.meetLink ?? ''],
          location: [interview.location ?? ''],
          notes: [interview.notes ?? ''],
        });
      });
      this.rescheduleForms.set(forms);
    });
  }
}
