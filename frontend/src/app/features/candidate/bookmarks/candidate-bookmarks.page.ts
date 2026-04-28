import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { JobResponse } from '../../../core/models/job.models';
import { BookmarksService } from '../../../core/services/bookmarks.service';
import { JobService } from '../../../core/services/job.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-candidate-bookmarks-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Saved jobs"
        title="Your shortlist of interesting roles"
        description="Bookmarks are stored locally so you can stage promising roles before applying."
      />

      @if (jobs().length) {
        <div class="surface-list">
          @for (job of jobs(); track job.jobId) {
            <article class="card-shell content-card">
              <div class="page-header">
                <div>
                  <span class="eyebrow">{{ job.category }}</span>
                  <h2>{{ job.title }}</h2>
                  <p>{{ job.location }}</p>
                </div>
                <app-status-pill [label]="job.status" />
              </div>
              <div class="button-row">
                <a class="primary-button" [routerLink]="['/candidate/jobs', job.jobId]">Open job</a>
                <button class="danger-button" type="button" (click)="bookmarks.toggle(job.jobId)">Remove</button>
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="SV" title="No saved jobs yet" description="Bookmark roles from the job marketplace to revisit them here." actionLabel="Browse jobs" actionLink="/candidate/jobs" />
      }
    </section>
  `,
})
export class CandidateBookmarksPageComponent {
  private readonly jobsService = inject(JobService);
  protected readonly bookmarks = inject(BookmarksService);

  protected readonly jobs = signal<JobResponse[]>([]);

  constructor() {
    effect(() => {
      const ids = this.bookmarks.bookmarkedJobIds();
      if (!ids.length) {
        this.jobs.set([]);
        return;
      }
      forkJoin(ids.map((jobId) => this.jobsService.getJob(jobId))).subscribe((jobs) => this.jobs.set(jobs));
    });
  }
}
