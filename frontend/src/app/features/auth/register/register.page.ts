import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { ROLE_LABELS, UserRole } from '../../../core/constants/role.constants';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

function matchingPasswords(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="auth-shell">
      <aside class="card-shell auth-aside">
        <span class="eyebrow">Launch a polished workflow</span>
        <h1>Create your HireConnect account</h1>
        <p>
          Start with a secure account, then complete the profile workspace that fits your hiring role.
        </p>
        <ul class="auth-benefits">
          <li>Candidate journeys with searchable jobs, applications, and interview tracking</li>
          <li>Recruiter controls for job posting, applicant review, and analytics</li>
          <li>Production-style Angular architecture ready for the next frontend phase</li>
        </ul>
      </aside>

      <section class="card-shell auth-card">
        <div class="page-header">
          <div>
            <span class="eyebrow">Join the platform</span>
            <h2>Register in under a minute</h2>
            <p>Choose your role carefully. It shapes your dashboard, permissions, and navigation.</p>
          </div>
        </div>

        <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
          <div class="field-block">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="abc@company.com" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <small>Please enter a valid email address.</small>
            }
          </div>

          <div class="form-grid">
            <div class="field-block">
              <label for="password">Password</label>
              <input id="password" type="password" formControlName="password" placeholder="Minimum 6 characters" />
              @if (form.controls.password.touched && form.controls.password.invalid) {
                <small>Password must be at least 6 characters long.</small>
              }
            </div>

            <div class="field-block">
              <label for="confirmPassword">Confirm Password</label>
              <input id="confirmPassword" type="password" formControlName="confirmPassword" placeholder="Repeat the password" />
              @if ((form.controls.confirmPassword.touched && form.controls.confirmPassword.invalid) || form.hasError('passwordMismatch')) {
                <small>Passwords must match.</small>
              }
            </div>
          </div>

          <div class="field-block">
            <label for="role">Role</label>
            <select id="role" formControlName="role">
              <option value="CANDIDATE">{{ roleLabels.CANDIDATE }}</option>
              <option value="RECRUITER">{{ roleLabels.RECRUITER }}</option>
            </select>
          </div>

          @if (errorMessage()) {
            <small>{{ errorMessage() }}</small>
          }

          <div class="form-actions">
            <button class="primary-button" type="submit" [disabled]="form.invalid || submitting()">
              {{ submitting() ? 'Creating account...' : 'Create account' }}
            </button>
            <a class="ghost-button" [href]="auth.buildGithubLoginUrl(form.controls.role.value)">Continue with GitHub</a>
          </div>
        </form>
      </section>
    </section>
  `,
})
export class RegisterPageComponent {
  protected readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      role: ['CANDIDATE' as Exclude<UserRole, 'ADMIN'>, [Validators.required]],
    },
    { validators: matchingPasswords },
  );

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    const { confirmPassword: _ignored, ...payload } = this.form.getRawValue();
    this.auth.register(payload).subscribe({
      next: (response) => {
        this.toast.success('Account created', 'You can finish your role profile now.');
        const nextRoute = response.user.role === 'RECRUITER' ? '/recruiter/profile' : '/candidate/profile';
        void this.router.navigateByUrl(nextRoute);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to create the account.'));
        this.toast.error('Registration failed', this.errorMessage());
      },
      complete: () => this.submitting.set(false),
    });
  }
}
