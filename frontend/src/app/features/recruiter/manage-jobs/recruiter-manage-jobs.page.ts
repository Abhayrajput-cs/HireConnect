import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { JobResponse } from '../../../core/models/job.models';
import { JobService } from '../../../core/services/job.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-recruiter-manage-jobs-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Recruiter jobs"
        title="Manage your published opportunities"
        description="Edit, review applicants, and close hiring once a role has reached its finish line."
        actionLabel="Create new job"
        actionLink="/recruiter/jobs/new"
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
                    <p>{{ job.location }} | {{ job.postedAt | date:'mediumDate' }}</p>
                  </div>
                </div>
                <app-status-pill [label]="job.status" />
              </div>
              <div class="meta-list">
                <div><span>Salary</span><strong>{{ job.salaryMin | currency:'INR':'symbol':'1.0-0' }} - {{ job.salaryMax | currency:'INR':'symbol':'1.0-0' }}</strong></div>
                <div><span>Experience</span><strong>{{ job.experienceRequired }} years</strong></div>
              </div>
              <div class="button-row">
                <a class="ghost-button" [routerLink]="['/recruiter/jobs', job.jobId, 'edit']">Edit</a>
                <a class="primary-button" [routerLink]="['/recruiter/jobs', job.jobId, 'applicants']">View applicants</a>
                @if (job.status !== 'CLOSED') {
                  <button class="danger-button" type="button" (click)="closeHiring(job)">Close hiring</button>
                } @else {
                  <button class="ghost-button" type="button" disabled>Hiring closed</button>
                }
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state icon="JB" title="No jobs yet" description="Create your first posting to start attracting candidates." actionLabel="Create job" actionLink="/recruiter/jobs/new" />
      }
    </section>
  `,
})
export class RecruiterManageJobsPageComponent {
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly jobsService = inject(JobService);
  private readonly toast = inject(ToastService);

  protected readonly jobs = signal<JobResponse[]>([]);

  constructor() {
    this.load();
  }

  protected closeHiring(job: JobResponse): void {
    this.jobsService.updateJob(job.jobId, {
      title: job.title,
      category: job.category,
      type: job.type,
      location: job.location,
      salaryMin: job.salaryMin,
      salaryMax: job.salaryMax,
      description: job.description,
      skills: job.skills,
      experienceRequired: job.experienceRequired,
      postedBy: job.postedBy,
      status: 'CLOSED',
      postedAt: job.postedAt,
    }).subscribe(() => {
      this.toast.success('Hiring closed', 'The job remains visible to candidates but no longer accepts applications.');
      this.load();
    });
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

  private load(): void {
    this.viewerProfile.getCurrentProfile(true).subscribe((profile) => {
      if (!profile) {
        this.jobs.set([]);
        return;
      }
      this.jobsService.getJobsByRecruiter(profile.profileId).subscribe((jobs) => this.jobs.set(jobs));
    });
  }
}
