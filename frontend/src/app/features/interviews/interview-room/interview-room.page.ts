import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { InterviewResponse } from '../../../core/models/interview.models';
import { InterviewService } from '../../../core/services/interview.service';
import { ToastService } from '../../../core/services/toast.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-interview-room-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section interview-room-page">
      <app-page-header
        eyebrow="Interview room"
        title="Join your scheduled interview"
        description="Review the slot details and open the interview link when you are ready."
      />

      @if (interview(); as item) {
        <article class="interview-room-card">
          <div class="interview-room-card__main">
            <span class="interview-room-card__icon material-symbols-rounded">video_camera_front</span>
            <div>
              <span class="eyebrow">Application {{ item.applicationId }}</span>
              <h2>{{ item.mode }} interview</h2>
              <p>{{ item.scheduledAt | date:'full' }}</p>
              <small>{{ item.notes || 'No extra notes were added for this interview.' }}</small>
            </div>
            <app-status-pill [label]="item.status" />
          </div>

          <div class="interview-room-details">
            <div>
              <span class="material-symbols-rounded">schedule</span>
              <label>Scheduled slot</label>
              <strong>{{ item.scheduledAt | date:'medium' }}</strong>
            </div>
            <div>
              <span class="material-symbols-rounded">meeting_room</span>
              <label>Mode</label>
              <strong>{{ item.mode }}</strong>
            </div>
            <div>
              <span class="material-symbols-rounded">link</span>
              <label>Meet link</label>
              <strong>{{ item.meetLink || 'Not shared yet' }}</strong>
            </div>
            <div>
              <span class="material-symbols-rounded">pin_drop</span>
              <label>Location</label>
              <strong>{{ item.location || 'Online / not required' }}</strong>
            </div>
          </div>

          <div class="interview-room-actions">
            @if (item.meetLink) {
              <a class="primary-button" [href]="normalizedMeetLink()" target="_blank" rel="noreferrer">
                <span class="material-symbols-rounded">open_in_new</span>
                Join interview
              </a>
            } @else {
              <button class="primary-button" type="button" disabled>Meet link pending</button>
            }
            <button class="ghost-button" type="button" (click)="goBack()">Back</button>
          </div>
        </article>
      } @else if (loaded()) {
        <app-empty-state icon="IV" title="Interview not found" description="This interview could not be loaded from interview-service." />
      }
    </section>
  `,
})
export class InterviewRoomPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly interviews = inject(InterviewService);
  private readonly toast = inject(ToastService);
  private readonly location = inject(Location);

  protected readonly interview = signal<InterviewResponse | null>(null);
  protected readonly loaded = signal(false);
  protected readonly normalizedMeetLink = computed(() => {
    const link = this.interview()?.meetLink?.trim();
    if (!link) {
      return null;
    }
    return /^https?:\/\//i.test(link) ? link : `https://${link}`;
  });

  constructor() {
    const interviewId = Number(this.route.snapshot.paramMap.get('interviewId'));
    if (!Number.isFinite(interviewId)) {
      this.loaded.set(true);
      return;
    }

    this.interviews.getById(interviewId).subscribe({
      next: (interview) => {
        this.interview.set(interview);
        this.loaded.set(true);
      },
      error: () => {
        this.loaded.set(true);
        this.toast.error('Interview unavailable', 'Could not load this interview room.');
      },
    });
  }

  protected goBack(): void {
    this.location.back();
  }
}
