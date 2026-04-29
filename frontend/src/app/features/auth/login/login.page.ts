import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ROLE_LABELS } from '../../../core/constants/role.constants';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-shell">
      <aside class="card-shell auth-aside">
        <span class="eyebrow">Access your workspace</span>
        <h1>Sign into HireConnect</h1>
        <p>
          Move from discovery to hiring decisions in one premium dashboard connected directly to your API Gateway.
        </p>
        <ul class="auth-benefits">
          <li>Role-aware dashboards for candidate, recruiter, and admin workspaces</li>
          <li>Gateway-backed data for jobs, interviews, applications, and analytics</li>
          <li>Secure session restore, refresh-token handling, and GitHub OAuth support</li>
        </ul>
      </aside>

      <section class="card-shell auth-card">
        <div class="page-header">
          <div>
            <span class="eyebrow">Welcome back</span>
            <h2>Continue your hiring workflow</h2>
            <p>Use your registered email and password, or continue with GitHub.</p>
          </div>
        </div>

        <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
          <div class="field-block">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="name@company.com" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <small>Please enter a valid email address.</small>
            }
          </div>

          <div class="field-block">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" placeholder="Your password" />
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <small>Password is required.</small>
            }
          </div>

          @if (errorMessage()) {
            <small>{{ errorMessage() }}</small>
          }

          <div class="form-actions">
            <button class="primary-button" type="submit" [disabled]="form.invalid || submitting()">
              {{ submitting() ? 'Signing in...' : 'Sign in' }}
            </button>
            <a class="ghost-button" routerLink="/register">Create account</a>
          </div>
        </form>

        <section class="stack" style="margin-top: 1.25rem;">
          <div class="page-header">
            <div>
              <span class="eyebrow">GitHub OAuth</span>
              <h3>Jump in faster</h3>
              <p>Choose the role you want GitHub sign-in to create or continue.</p>
            </div>
          </div>

          <div class="button-row">
            @for (role of oauthRoles(); track role) {
              <a class="ghost-button" [href]="auth.buildGithubLoginUrl(role)">
                Continue as {{ roleLabels[role] }}
              </a>
            }
          </div>
        </section>
      </section>
    </section>
  `,
})
export class LoginPageComponent {
  protected readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly oauthRoles = computed(() => ['CANDIDATE', 'RECRUITER'] as const);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    this.auth.login(this.form.getRawValue()).pipe(
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: (response) => {
        this.toast.success('Welcome back', `Signed in as ${response.user.email}`);
        this.auth.redirectToRoleHome(response.user.role);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to sign in.'));
        this.toast.error('Login failed', this.errorMessage());
      },
    });
  }
}
