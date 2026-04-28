import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="empty-state card-shell">
      <span class="empty-state__icon">NO</span>
      <h1>Unauthorized</h1>
      <p>This workspace route is protected for a different role.</p>
      <div class="button-row">
        <a class="primary-button" routerLink="/dashboard">Go to my dashboard</a>
        <a class="ghost-button" routerLink="/">Back home</a>
      </div>
    </section>
  `,
})
export class UnauthorizedPageComponent {}
