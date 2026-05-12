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

        @if (resetMode()) {
          <form class="stack" [formGroup]="resetForm" (ngSubmit)="submitReset()">
            <div class="notice-card">
              <span class="eyebrow">Password recovery</span>
              <h3>Reset with your verified email</h3>
              <p>We send a 6-digit reset code only to verified HireConnect accounts.</p>
            </div>

            <div class="field-block">
              <label for="resetEmail">Email</label>
              <input id="resetEmail" type="email" formControlName="email" placeholder="name@company.com" />
              @if (resetForm.controls.email.touched && resetForm.controls.email.invalid) {
                <small>Please enter a valid verified email.</small>
              }
            </div>

            @if (resetCodeSent()) {
              <div class="field-block">
                <label for="resetCode">Reset code</label>
                <input id="resetCode" type="text" inputmode="numeric" maxlength="6" formControlName="code" placeholder="123456" />
                @if (resetForm.controls.code.touched && resetForm.controls.code.invalid) {
                  <small>Enter the 6-digit code.</small>
                }
              </div>

              <div class="field-block">
                <label for="newPassword">New password</label>
                <input id="newPassword" type="password" formControlName="newPassword" placeholder="Minimum 6 characters" />
                @if (resetForm.controls.newPassword.touched && resetForm.controls.newPassword.invalid) {
                  <small>Password must be at least 6 characters long.</small>
                }
              </div>
            }

            @if (errorMessage()) {
              <small>{{ errorMessage() }}</small>
            }

            <div class="form-actions">
              <button class="primary-button" type="submit" [disabled]="submitting()">
                {{ submitting() ? 'Processing...' : resetCodeSent() ? 'Update password' : 'Send reset code' }}
              </button>
              <button class="ghost-button" type="button" [disabled]="submitting()" (click)="showLogin()">Back to sign in</button>
            </div>
          </form>
        } @else {
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

          <button class="link-button" type="button" (click)="showReset()">Forgot password?</button>

          <div class="form-actions">
            <button class="primary-button" type="submit" [disabled]="submitting()">
              {{ submitting() ? 'Signing in...' : 'Sign in' }}
            </button>
            <a class="ghost-button" routerLink="/register">Create account</a>
          </div>
        </form>
        }

        @if (!resetMode()) {
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
        }
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
  protected readonly resetMode = signal(false);
  protected readonly resetCodeSent = signal(false);
  protected readonly oauthRoles = computed(() => ['CANDIDATE', 'RECRUITER'] as const);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly resetForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    code: ['', [Validators.pattern(/^\d{6}$/)]],
    newPassword: ['', [Validators.minLength(6)]],
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
        this.toast.success('Welcome back', `Signed in as ${response.user.fullName || response.user.email}`);
        this.auth.redirectToRoleHome(response.user.role);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to sign in.'));
        this.toast.error('Login failed', this.errorMessage());
      },
    });
  }

  protected showReset(): void {
    this.errorMessage.set('');
    this.resetMode.set(true);
    this.resetCodeSent.set(false);
    this.resetForm.patchValue({ email: this.form.controls.email.value, code: '', newPassword: '' });
  }

  protected showLogin(): void {
    this.errorMessage.set('');
    this.resetMode.set(false);
    this.resetCodeSent.set(false);
  }

  protected submitReset(): void {
    if (this.submitting()) {
      return;
    }
    if (!this.resetCodeSent()) {
      if (this.resetForm.controls.email.invalid) {
        this.resetForm.controls.email.markAsTouched();
        return;
      }
      this.errorMessage.set('');
      this.submitting.set(true);
      this.auth.forgotPassword({ email: this.resetForm.controls.email.value }).pipe(
        finalize(() => this.submitting.set(false)),
      ).subscribe({
        next: (response) => {
          this.resetCodeSent.set(true);
          this.toast.success('Reset code sent', response.message);
        },
        error: (error: unknown) => {
          this.errorMessage.set(getErrorMessage(error, 'Unable to send reset code.'));
          this.toast.error('Reset failed', this.errorMessage());
        },
      });
      return;
    }

    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }
    this.errorMessage.set('');
    this.submitting.set(true);
    this.auth.resetPassword({
      email: this.resetForm.controls.email.value,
      code: this.resetForm.controls.code.value,
      newPassword: this.resetForm.controls.newPassword.value,
    }).pipe(
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: (response) => {
        this.toast.success('Password updated', response.message);
        this.form.patchValue({ email: this.resetForm.controls.email.value, password: '' });
        this.showLogin();
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to reset password.'));
        this.toast.error('Reset failed', this.errorMessage());
      },
    });
  }
}
