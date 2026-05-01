import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { JobResponse } from '../../../core/models/job.models';
import { JobService } from '../../../core/services/job.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-admin-jobs-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Jobs"
        title="Platform job inventory"
        description="This page reflects the read-side job-service contract currently available to the frontend."
      />

      @if (jobs().length) {
        <div class="surface-list">
          @for (job of jobs(); track job.jobId) {
            <article class="workspace-panel">
              <div class="notification-row">
                <div class="job-market-card__top">
                  <span class="company-mark">{{ companyMark(job.title) }}</span>
                  <div>
                    <span class="eyebrow">{{ job.category }}</span>
                    <h2>{{ job.title }}</h2>
                    <p>{{ job.location }} | Recruiter profile {{ job.postedBy }}</p>
                  </div>
                </div>
                <app-status-pill [label]="job.status" />
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="JB" title="No jobs visible" description="Open roles will appear here when job-service data is accessible to the admin session." />
      }
    </section>
  `,
})
export class AdminJobsPageComponent {
  private readonly jobsService = inject(JobService);
  protected readonly jobs = signal<JobResponse[]>([]);

  constructor() {
    this.jobsService.getJobs().subscribe((jobs) => this.jobs.set(jobs));
  }

  protected companyMark(title: string): string {
    return title
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  }
}
