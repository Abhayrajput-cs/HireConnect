import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { SessionService } from '../core/services/session.service';
import { ThemeToggleComponent } from '../shared/components/theme-toggle/theme-toggle.component';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ThemeToggleComponent],
  template: `
    <div class="public-shell">
      <header class="public-shell__header">
        <a routerLink="/" class="brand-lockup">
          <span class="brand-lockup__mark">HC</span>
          <div>
            <strong>HireConnect</strong>
            <small>Talent network for modern hiring teams</small>
          </div>
        </a>

        <nav class="public-shell__nav">
          <a routerLink="/" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">Home</a>
          <a routerLink="/" fragment="job-categories">Jobs</a>
          <a routerLink="/" fragment="employers">For employers</a>
          <a routerLink="/" fragment="stories">Success stories</a>
          <app-theme-toggle />
          @if (session.isAuthenticated()) {
            <a [routerLink]="session.roleHome()" class="ghost-button">Open workspace</a>
          } @else {
            <a routerLink="/login" routerLinkActive="is-active">Login</a>
            <a routerLink="/register" class="primary-button">Register</a>
          }
        </nav>
      </header>

      <main class="public-shell__content">
        <router-outlet />
      </main>

      <footer class="public-footer card-shell">
        <div class="public-footer__brand">
          <a routerLink="/" class="brand-lockup">
            <span class="brand-lockup__mark">HC</span>
            <div>
              <strong>HireConnect</strong>
              <small>Career growth for candidates. Hiring velocity for recruiters.</small>
            </div>
          </a>
          <div class="public-footer__socials">
            <a href="https://www.linkedin.com" target="_blank" rel="noreferrer">LinkedIn</a>
            <a href="https://www.instagram.com" target="_blank" rel="noreferrer">Instagram</a>
            <a href="https://www.youtube.com" target="_blank" rel="noreferrer">YouTube</a>
          </div>
        </div>

        <div class="public-footer__links">
          <div>
            <strong>Explore</strong>
            <a routerLink="/" fragment="job-categories">Jobs</a>
            <a routerLink="/" fragment="stories">Success stories</a>
            <a routerLink="/register">Create account</a>
          </div>
          <div>
            <strong>Employers</strong>
            <a routerLink="/" fragment="employers">Why HireConnect</a>
            <a routerLink="/register">Recruiter signup</a>
            <a routerLink="/login">Employer login</a>
          </div>
          <div>
            <strong>Support</strong>
            <a routerLink="/">Help center</a>
            <a routerLink="/">Privacy policy</a>
            <a routerLink="/">Terms and conditions</a>
          </div>
        </div>
      </footer>
    </div>
  `,
})
export class PublicLayoutComponent {
  protected readonly session = inject(SessionService);
}
