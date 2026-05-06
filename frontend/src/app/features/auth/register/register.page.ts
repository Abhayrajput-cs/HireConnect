import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
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
          @if (verificationEmail()) {
            <div class="notice-card">
              <span class="eyebrow">Verify email</span>
              <h3>Enter the 6-digit code sent to {{ verificationEmail() }}</h3>
              <p>Only verified inboxes can sign in to HireConnect.</p>
            </div>

            <div class="field-block">
              <label for="verificationCode">Verification code</label>
              <input id="verificationCode" type="text" inputmode="numeric" maxlength="6" [formControl]="verificationCode" placeholder="123456" />
              @if (verificationCode.touched && verificationCode.invalid) {
                <small>Enter the 6-digit email code.</small>
              }
            </div>
          } @else {
          <div class="field-block">
            <label for="fullName">Full name</label>
            <input id="fullName" type="text" formControlName="fullName" placeholder="Your full name" />
            @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
              <small>Enter your real full name.</small>
            }
          </div>

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
                <small>Password must be at least 8 characters and include uppercase, lowercase, and a number.</small>
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
          }

          @if (errorMessage()) {
            <small>{{ errorMessage() }}</small>
          }

          <div class="form-actions">
            <button class="primary-button" type="submit" [disabled]="submitting()">
              {{ submitLabel() }}
            </button>
            @if (verificationEmail()) {
              <button class="ghost-button" type="button" [disabled]="submitting()" (click)="resendCode()">Resend code</button>
            } @else {
              <a class="ghost-button" [href]="auth.buildGithubLoginUrl(form.controls.role.value)">Continue with GitHub</a>
            }
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
  protected readonly verificationEmail = signal('');
  protected readonly verificationCode = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^\d{6}$/)],
  });
  protected readonly submitLabel = computed(() => {
    if (this.submitting()) {
      return this.verificationEmail() ? 'Verifying...' : 'Creating account...';
    }
    return this.verificationEmail() ? 'Verify and continue' : 'Create account';
  });

  protected readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120), Validators.pattern(/^[A-Za-z][A-Za-z .'-]*$/)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/)]],
      confirmPassword: ['', [Validators.required]],
      role: ['CANDIDATE' as Exclude<UserRole, 'ADMIN'>, [Validators.required]],
    },
    { validators: matchingPasswords },
  );

  protected submit(): void {
    if (this.verificationEmail()) {
      this.verifyCode();
      return;
          }
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    const { confirmPassword: _ignored, ...payload } = this.form.getRawValue();
    this.auth.register(payload).subscribe({
      next: (response) => {
        this.verificationEmail.set(response.email);
        this.toast.success('Verification sent', response.message);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to create the account.'));
        this.toast.error('Registration failed', this.errorMessage());
      },
      complete: () => this.submitting.set(false),
    });
  }

  protected resendCode(): void {
    if (!this.verificationEmail() || this.submitting()) {
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    this.auth.resendVerification({ email: this.verificationEmail() }).subscribe({
      next: (response) => this.toast.success('Verification sent', response.message),
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to resend verification code.'));
        this.toast.error('Verification failed', this.errorMessage());
      },
      complete: () => this.submitting.set(false),
    });
  }

  private verifyCode(): void {
    if (this.verificationCode.invalid || this.submitting()) {
      this.verificationCode.markAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    this.auth.verifyEmail({ email: this.verificationEmail(), code: this.verificationCode.value }).subscribe({
      next: (response) => {
        this.toast.success('Email verified', 'Your workspace is ready.');
        const nextRoute = response.user.role === 'RECRUITER' ? '/recruiter/profile' : '/candidate/profile';
        void this.router.navigateByUrl(nextRoute);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to verify this email.'));
        this.toast.error('Verification failed', this.errorMessage());
      },
      complete: () => this.submitting.set(false),
    });
  }
}
