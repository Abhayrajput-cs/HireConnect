import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { JobResponse } from '../../../core/models/job.models';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { BookmarksService } from '../../../core/services/bookmarks.service';
import { JobService } from '../../../core/services/job.service';
import { SessionService } from '../../../core/services/session.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-candidate-jobs-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CurrencyPipe, PageHeaderComponent, EmptyStateComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Job marketplace"
        title="Search roles that match your momentum"
        description="Filtered directly against job-service search parameters exposed through the API Gateway."
      />

      <section class="card-shell content-card">
        <form class="form-grid form-grid--wide" [formGroup]="filters" (ngSubmit)="loadJobs()">
          <div class="field-block">
            <label>Title</label>
            <input formControlName="title" placeholder="Backend Engineer" />
          </div>
          <div class="field-block">
            <label>Category</label>
            <input formControlName="category" placeholder="Engineering" />
          </div>
          <div class="field-block">
            <label>Location</label>
            <input formControlName="location" placeholder="Pune" />
          </div>
          <div class="field-block">
            <label>Experience</label>
            <input type="number" formControlName="experienceRequired" />
          </div>
          <div class="field-block">
            <label>Status</label>
            <select formControlName="status">
              <option value="">All jobs</option>
              <option value="OPEN">Open</option>
              <option value="PAUSED">Paused</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
          <div class="field-block">
            <label>Salary max</label>
            <input type="number" formControlName="salaryMax" />
          </div>
          <div class="form-actions">
            <button class="primary-button" type="submit">Search jobs</button>
            <button class="ghost-button" type="button" (click)="resetFilters()">Reset</button>
          </div>
        </form>
      </section>

      @if (errorMessage()) {
        <small>{{ errorMessage() }}</small>
      }

      @if (jobs().length) {
        <section class="panel-grid">
          @for (job of jobs(); track job.jobId) {
            <article class="card-shell content-card">
              <div class="page-header">
                <div>
                  <span class="eyebrow">{{ job.category }}</span>
                  <h2>{{ job.title }}</h2>
                  <p>{{ job.location }} | {{ job.type }}</p>
                </div>
                <app-status-pill [label]="job.status" />
              </div>

              <p class="muted">{{ job.description }}</p>

              <div class="chip-group">
                @for (skill of job.skills; track skill) {
                  <span class="chip">{{ skill }}</span>
                }
              </div>

              <div class="meta-list">
                <div><span>Salary range</span><strong>{{ job.salaryMin | currency:'INR':'symbol':'1.0-0' }} - {{ job.salaryMax | currency:'INR':'symbol':'1.0-0' }}</strong></div>
                <div><span>Experience</span><strong>{{ job.experienceRequired }} years</strong></div>
              </div>

              @if (job.status === 'CLOSED') {
                <div class="job-state-banner job-state-banner--closed">
                  <span class="material-symbols-rounded">event_busy</span>
                  <div>
                    <strong>Applications closed</strong>
                    <p>Hiring is closed for this role. You can still open the job and review its details.</p>
                  </div>
                </div>
              }

              <div class="button-row">
                <a class="primary-button" [routerLink]="['/candidate/jobs', job.jobId]">View details</a>
                <button class="ghost-button" type="button" (click)="toggleBookmark(job.jobId)">
                  {{ bookmarks.isBookmarked(job.jobId) ? 'Remove bookmark' : 'Save job' }}
                </button>
              </div>
            </article>
          }
        </section>
      } @else {
        <app-empty-state icon="JB" title="No jobs match the current filters" description="Try a broader location, category, or title to surface more opportunities." />
      }
    </section>
  `,
})
export class CandidateJobsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly jobsService = inject(JobService);
  private readonly analytics = inject(AnalyticsService);
  protected readonly bookmarks = inject(BookmarksService);
  private readonly session = inject(SessionService);

  protected readonly jobs = signal<JobResponse[]>([]);
  protected readonly errorMessage = signal('');
  protected readonly filters = this.fb.nonNullable.group({
    title: [''],
    category: [''],
    location: [''],
    salaryMax: [0],
    experienceRequired: [0],
    status: [''],
  });

  constructor() {
    this.loadJobs();
  }

  protected loadJobs(): void {
    this.errorMessage.set('');
    const raw = this.filters.getRawValue();
    this.jobsService.getJobs({
      title: raw.title || null,
      category: raw.category || null,
      location: raw.location || null,
      salaryMax: raw.salaryMax || null,
      experienceRequired: raw.experienceRequired || null,
      status: raw.status || null,
    }).subscribe({
      next: (jobs) => this.jobs.set(jobs),
      error: (error: unknown) => this.errorMessage.set(getErrorMessage(error, 'Unable to load jobs.')),
    });
  }

  protected resetFilters(): void {
    this.filters.setValue({
      title: '',
      category: '',
      location: '',
      salaryMax: 0,
      experienceRequired: 0,
      status: '',
    });
    this.loadJobs();
  }

  protected toggleBookmark(jobId: number): void {
    this.bookmarks.toggle(jobId);
    void this.analytics.recordJobView(jobId, { viewerId: this.session.user()?.userId }).subscribe();
  }
}
