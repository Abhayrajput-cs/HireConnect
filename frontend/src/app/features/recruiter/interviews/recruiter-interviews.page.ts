import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { ApplicationResponse } from '../../../core/models/application.models';
import { InterviewResponse } from '../../../core/models/interview.models';
import { JobResponse } from '../../../core/models/job.models';
import { ProfileResponse } from '../../../core/models/profile.models';
import { ApplicationService } from '../../../core/services/application.service';
import { InterviewService } from '../../../core/services/interview.service';
import { JobService } from '../../../core/services/job.service';
import { ProfileService } from '../../../core/services/profile.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

interface RecruiterInterviewView {
  interview: InterviewResponse;
  application: ApplicationResponse;
  job: JobResponse;
  candidate: ProfileResponse | null;
}

@Component({
  selector: 'app-recruiter-interviews-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section recruiter-interviews-page">
      <app-page-header
        eyebrow="Interviews"
        title="Scheduled interview rooms"
        description="Track candidate interview slots, open join rooms, and keep the hiring conversation moving."
      />

      @if (interviews().length) {
        <section class="interview-command-grid">
          @for (item of interviews(); track item.interview.interviewId) {
            <article class="interview-command-card">
              <div class="interview-command-card__top">
                <span class="interview-room-card__icon material-symbols-rounded">video_camera_front</span>
                <div>
                  <span class="eyebrow">Application {{ item.application.applicationId }}</span>
                  <h2>{{ item.candidate?.fullName || item.candidate?.email || 'Candidate' }}</h2>
                  <p>{{ item.job.title }}</p>
                </div>
                <app-status-pill [label]="item.interview.status" />
              </div>

              <div class="interview-room-details">
                <div>
                  <span class="material-symbols-rounded">schedule</span>
                  <label>Slot</label>
                  <strong>{{ item.interview.scheduledAt | date:'medium' }}</strong>
                </div>
                <div>
                  <span class="material-symbols-rounded">meeting_room</span>
                  <label>Mode</label>
                  <strong>{{ item.interview.mode }}</strong>
                </div>
                <div>
                  <span class="material-symbols-rounded">link</span>
                  <label>Meet link</label>
                  <strong>{{ item.interview.meetLink || 'Not shared' }}</strong>
                </div>
                <div>
                  <span class="material-symbols-rounded">pin_drop</span>
                  <label>Location</label>
                  <strong>{{ item.interview.location || 'Online / not required' }}</strong>
                </div>
              </div>

              <div class="button-row">
                <a class="primary-button" [routerLink]="['/recruiter/interviews', item.interview.interviewId, 'join']">
                  <span class="material-symbols-rounded">open_in_new</span>
                  Join room
                </a>
                @if (item.application.resumeUrl || item.candidate?.resumeUrl) {
                  <a class="ghost-button" [href]="item.application.resumeUrl || item.candidate?.resumeUrl" target="_blank" rel="noreferrer">View resume</a>
                }
                <a class="ghost-button" [routerLink]="['/recruiter/jobs', item.job.jobId, 'applicants']">View applicant</a>
              </div>
            </article>
          }
        </section>
      } @else {
        <app-empty-state icon="IV" title="No interviews scheduled" description="Once you schedule an applicant interview, it will appear here with a join room." />
      }
    </section>
  `,
})
export class RecruiterInterviewsPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly jobs = inject(JobService);
  private readonly applications = inject(ApplicationService);
  private readonly interviewsService = inject(InterviewService);
  private readonly profiles = inject(ProfileService);

  protected readonly interviews = signal<RecruiterInterviewView[]>([]);

  constructor() {
    this.load();
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile(true).pipe(
      switchMap((profile) => {
        if (!profile) {
          return of([] as RecruiterInterviewView[]);
        }
        return this.jobs.getJobsByRecruiter(profile.profileId).pipe(
          switchMap((jobs) => {
            if (!jobs.length) {
              return of([] as RecruiterInterviewView[]);
            }
            return forkJoin(jobs.map((job) => this.loadJobInterviews(job))).pipe(
              map((items) => items.flat().sort((a, b) =>
                new Date(a.interview.scheduledAt).getTime() - new Date(b.interview.scheduledAt).getTime(),
              )),
            );
          }),
        );
      }),
    ).subscribe((items) => this.interviews.set(items));
  }

  private loadJobInterviews(job: JobResponse) {
    return this.applications.getByJob(job.jobId).pipe(
      switchMap((applications) => {
        if (!applications.length) {
          return of([] as RecruiterInterviewView[]);
        }
        return forkJoin(applications.map((application) => this.loadApplicationInterviews(job, application))).pipe(
          map((items) => items.flat()),
        );
      }),
      catchError(() => of([] as RecruiterInterviewView[])),
    );
  }

  private loadApplicationInterviews(job: JobResponse, application: ApplicationResponse) {
    return forkJoin({
      candidate: this.profiles.getProfileById(application.candidateId).pipe(catchError(() => of(null))),
      interviews: this.interviewsService.getByApplication(application.applicationId).pipe(catchError(() => of([] as InterviewResponse[]))),
    }).pipe(
      map(({ candidate, interviews }) => interviews.map((interview) => ({
        interview,
        application,
        job,
        candidate,
      }))),
    );
  }
}
