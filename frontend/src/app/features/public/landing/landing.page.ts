import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { JobService } from '../../../core/services/job.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="public-hero">
      <div class="public-hero__copy">
        <span class="eyebrow">Find roles, grow careers, and hire with confidence</span>
        <h1>India's modern hiring home for candidates and recruiters</h1>
        <p>
          Browse what HireConnect does before you sign in. Explore high-demand categories, understand the hiring flow,
          and step into a premium role-based workspace the moment you are ready to apply or hire.
        </p>

        <div class="public-search card-shell">
          <div class="public-search__field">
            <span class="material-symbols-rounded">manage_search</span>
            <div>
              <strong>Roles and skills</strong>
              <small>Backend, Angular, Product, Data</small>
            </div>
          </div>
          <div class="public-search__field">
            <span class="material-symbols-rounded">location_on</span>
            <div>
              <strong>Locations</strong>
              <small>Remote, Noida, Bengaluru, Pune</small>
            </div>
          </div>
          <a class="primary-button" [routerLink]="primaryCtaLink()">{{ primaryCtaLabel() }}</a>
        </div>

      <div class="hero__actions">
          <a class="ghost-button" routerLink="/" fragment="job-categories">Browse sectors</a>
          <a class="ghost-button" routerLink="/" fragment="employers">For employers</a>
        </div>
      </div>

      <div class="public-hero__spotlight">
        <article class="hero-card card-shell">
          <div class="hero-card__pill">
            <span class="material-symbols-rounded">verified</span>
            <strong>Secure role marketplace</strong>
          </div>

          <div class="hero-card__stats">
            <article>
              <span>Candidate tools</span>
              <strong>Jobs, applications, interviews</strong>
            </article>
            <article>
              <span>Recruiter tools</span>
              <strong>Postings, applicants, analytics</strong>
            </article>
            <article>
              <span>Live marketplace</span>
              <strong>{{ secureMarketLabel() }}</strong>
            </article>
          </div>

          <div class="hero-card__signal">
            <span class="eyebrow">How it works</span>
            <h3>Preview the platform publicly. Apply only after login.</h3>
            <p>
              The backend keeps applications protected. The frontend explains the platform openly, then gates hiring
              actions when identity matters.
            </p>
          </div>
        </article>
      </div>
    </section>

    <section class="landing-metrics helper-grid">
      <article class="card-shell content-card landing-metric">
        <span class="material-symbols-rounded">work_history</span>
        <div>
          <strong>{{ openRolesLabel() }}</strong>
          <h3>Open hiring now</h3>
          <p>Visible opportunities flowing through the hiring marketplace.</p>
        </div>
      </article>
      <article class="card-shell content-card landing-metric">
        <span class="material-symbols-rounded">event_busy</span>
        <div>
          <strong>{{ closedRolesLabel() }}</strong>
          <h3>Recently closed roles</h3>
          <p>Public visitors can understand market movement before they log in.</p>
        </div>
      </article>
      @for (metric of metrics; track metric.label) {
        <article class="card-shell content-card landing-metric">
          <span class="material-symbols-rounded">{{ metric.icon }}</span>
          <div>
            <strong>{{ metric.value }}</strong>
            <h3>{{ metric.label }}</h3>
            <p>{{ metric.description }}</p>
          </div>
        </article>
      }
    </section>

    <section class="landing-section" id="job-categories">
      <div class="page-header">
        <div>
          <span class="eyebrow">Explore by focus</span>
          <h2>Popular hiring lanes on HireConnect</h2>
          <p>Built to feel like a real job marketplace, not a plain CRUD dashboard.</p>
        </div>
      </div>

      <div class="landing-category-grid">
        @for (category of categories; track category.title) {
          <article class="card-shell landing-category">
            <span class="material-symbols-rounded">{{ category.icon }}</span>
            <h3>{{ category.title }}</h3>
            <p>{{ category.copy }}</p>
            <small>{{ category.meta }}</small>
          </article>
        }
      </div>
    </section>

    <section class="landing-section landing-section--split" id="employers">
      <article class="card-shell content-card">
        <span class="eyebrow">For employers</span>
        <h2>Run hiring from one polished recruiter workspace</h2>
        <div class="surface-list">
          <article>
            <h3>Post and manage live openings</h3>
            <p>Create jobs, review applicants, and schedule interviews from one place.</p>
          </article>
          <article>
            <h3>Track pipeline movement</h3>
            <p>Interview updates, application progression, and analytics stay visible in one flow.</p>
          </article>
          <article>
            <h3>Respond faster with cleaner UI</h3>
            <p>Better recruiter pages reduce clutter and make applicant review easier.</p>
          </article>
        </div>
      </article>

      <article class="card-shell content-card landing-rail">
        <span class="eyebrow">Platform story</span>
        <h2>Why teams choose HireConnect</h2>
        <div class="chip-group">
          @for (brand of brands; track brand) {
            <span class="chip chip--brand">{{ brand }}</span>
          }
        </div>
        <p>
          The frontend is built around the same gateway-driven backend contracts used by the live microservices stack,
          so this experience is demo-ready and product-like instead of mocked.
        </p>
      </article>
    </section>

    <section class="landing-section" id="stories">
      <div class="page-header">
        <div>
          <span class="eyebrow">Success stories</span>
          <h2>Candidate and recruiter journeys built into one system</h2>
        </div>
      </div>

      <div class="panel-grid">
        @for (story of stories; track story.title) {
          <article class="card-shell content-card landing-story">
            <span class="material-symbols-rounded">{{ story.icon }}</span>
            <h3>{{ story.title }}</h3>
            <p>{{ story.copy }}</p>
          </article>
        }
      </div>
    </section>
  `,
})
export class LandingPageComponent {
  private readonly session = inject(SessionService);
  private readonly jobs = inject(JobService);

  protected readonly liveOpenRoles = signal<number | null>(null);
  protected readonly liveClosedRoles = signal<number | null>(null);
  protected readonly secureMarketLabel = computed(() =>
    this.liveOpenRoles() !== null ? `${this.liveOpenRoles()} live open roles` : 'Sign in to unlock live role counts',
  );
  protected readonly openRolesLabel = computed(() =>
    this.liveOpenRoles() !== null ? `${this.liveOpenRoles()} roles` : 'Live count available when exposed',
  );
  protected readonly closedRolesLabel = computed(() =>
    this.liveClosedRoles() !== null ? `${this.liveClosedRoles()} roles` : 'Status visibility built in',
  );

  protected readonly metrics = [
    {
      icon: 'monitoring',
      value: 'Role-based',
      label: 'Candidate, recruiter, and admin workspaces',
      description: 'Each workflow has its own polished dashboard instead of a one-size-fits-all screen.',
    },
    {
      icon: 'travel_explore',
      value: 'Secure browse',
      label: 'Open discovery, protected apply flow',
      description: 'Users can understand the platform publicly, then authenticate before taking hiring actions.',
    },
    {
      icon: 'calendar_month',
      value: 'Interview ready',
      label: 'Scheduling and rescheduling built in',
      description: 'Candidate and recruiter flows already reflect interview lifecycle movement.',
    },
  ];

  protected readonly categories = [
    { icon: 'code', title: 'Engineering', copy: 'Backend, full stack, cloud, and platform roles.', meta: 'Java, Angular, Spring Boot' },
    { icon: 'query_stats', title: 'Analytics', copy: 'Data reporting, business intelligence, and product insights.', meta: 'SQL, BI, dashboards' },
    { icon: 'campaign', title: 'Marketing', copy: 'Growth, brand, and digital campaign roles.', meta: 'Performance and storytelling' },
    { icon: 'account_balance', title: 'Fintech & Banking', copy: 'Operations, compliance, and financial technology hiring.', meta: 'Risk, payments, lending' },
    { icon: 'workspaces', title: 'Remote & Hybrid', copy: 'Flexible work setups for distributed teams.', meta: 'Remote-friendly opportunities' },
    { icon: 'school', title: 'Early career', copy: 'Fresher, internship, and stepping-stone roles.', meta: 'Internships and launchpads' },
  ];

  protected readonly brands = ['TCS', 'Infosys', 'Accenture', 'Wipro', 'Zomato', 'Razorpay'];

  protected readonly stories = [
    {
      icon: 'person_search',
      title: 'Candidates move from profile to offer',
      copy: 'Profiles, applications, interview updates, and notifications are all connected into one journey.',
    },
    {
      icon: 'domain_add',
      title: 'Recruiters post roles and track demand',
      copy: 'Openings, applicant flow, view-to-apply ratios, and interview progress stay close together.',
    },
    {
      icon: 'admin_panel_settings',
      title: 'Admins keep the platform visible',
      copy: 'User management and platform analytics give the backend stack a clear command layer.',
    },
  ];

  constructor() {
    forkJoin({
      openJobs: this.jobs.getJobs({ status: 'OPEN' }).pipe(catchError(() => of([]))),
      closedJobs: this.jobs.getJobs({ status: 'CLOSED' }).pipe(catchError(() => of([]))),
    }).subscribe((result) => {
      this.liveOpenRoles.set(result.openJobs.length || null);
      this.liveClosedRoles.set(result.closedJobs.length || null);
    });
  }

  protected primaryCtaLabel(): string {
    return this.session.isAuthenticated() ? 'Open workspace' : 'Login to apply';
  }

  protected primaryCtaLink(): string {
    return this.session.isAuthenticated() ? this.session.roleHome() : '/login';
  }
}
