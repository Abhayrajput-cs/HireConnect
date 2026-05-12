import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-role-home-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="card-shell content-card">
      <p class="eyebrow">Redirecting</p>
      <h2>Opening your workspace...</h2>
    </section>
  `,
})
export class RoleHomePageComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  constructor() {
    void this.router.navigateByUrl(this.session.roleHome());
  }
}
