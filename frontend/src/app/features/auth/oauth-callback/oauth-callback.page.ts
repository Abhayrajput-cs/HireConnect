import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-oauth-callback-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="card-shell content-card">
      <span class="eyebrow">GitHub OAuth</span>
      <h2>Finishing your sign-in...</h2>
      <p class="muted">We are validating the callback and restoring your session.</p>
    </section>
  `,
})
export class OAuthCallbackPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  constructor() {
    const params = new URLSearchParams(this.route.snapshot.queryParams as Record<string, string>);
    this.auth.completeOAuthCallback(params).subscribe({
      next: (user) => {
        if (!user) {
          this.toast.error('OAuth failed', 'Missing callback data from GitHub.');
          void this.router.navigate(['/login']);
          return;
        }

        this.toast.success('GitHub connected', `Signed in as ${user.email}`);
        this.auth.redirectToRoleHome(user.role);
      },
      error: () => {
        this.toast.error('OAuth failed', 'Unable to complete the GitHub sign-in flow.');
        void this.router.navigate(['/login']);
      },
    });
  }
}
