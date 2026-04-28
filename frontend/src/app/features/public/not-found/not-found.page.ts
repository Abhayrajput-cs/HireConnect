import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="empty-state card-shell">
      <span class="empty-state__icon">404</span>
      <h1>We could not find that page.</h1>
      <p>The route may be outdated, mistyped, or not exposed through the current backend contract.</p>
      <div class="button-row">
        <a class="primary-button" routerLink="/">Go home</a>
        <a class="ghost-button" routerLink="/dashboard">Open dashboard</a>
      </div>
    </section>
  `,
})
export class NotFoundPageComponent {}
