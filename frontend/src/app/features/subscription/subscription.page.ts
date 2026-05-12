import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { PaymentRole, SubscriptionPlanResponse, SubscriptionStatusResponse } from '../../core/models/payment.models';
import { ProfileResponse } from '../../core/models/profile.models';
import { PaymentService } from '../../core/services/payment.service';
import { SessionService } from '../../core/services/session.service';
import { ToastService } from '../../core/services/toast.service';
import { ViewerProfileService } from '../../core/services/viewer-profile.service';
import { getErrorMessage } from '../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { StatusPillComponent } from '../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-subscription-page',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent, PageHeaderComponent, StatusPillComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Subscription"
        [title]="role() === 'RECRUITER' ? 'Upgrade your hiring command center' : 'Boost your candidate profile'"
        [description]="role() === 'RECRUITER'
          ? 'Unlock premium candidate access, stronger filters, and richer hiring analytics.'
          : 'Get featured profile visibility, priority applications, and application analytics.'"
      />

      @if (status(); as currentStatus) {
        <section class="workspace-panel subscription-status-card">
          <div>
            <span class="eyebrow">Current plan</span>
            <h2>{{ currentStatus.planType || freePlanLabel() }}</h2>
            <p>{{ currentStatus.premiumActive ? 'Premium is active for this workspace.' : 'You are currently on the free workspace.' }}</p>
          </div>
          <app-status-pill [label]="currentStatus.status" />
          @if (currentStatus.expiryDate) {
            <strong>Valid until {{ currentStatus.expiryDate | date:'mediumDate' }}</strong>
          }
          @if (currentStatus.premiumActive && profileId()) {
            <button class="ghost-button" type="button" (click)="cancel()">Cancel subscription</button>
          }
        </section>
      }

      @if (plans().length) {
        <section class="subscription-plan-grid">
          @for (plan of plans(); track plan.planType) {
            <article class="workspace-panel subscription-plan-card" [class.is-premium]="plan.premium">
              <div class="subscription-plan-card__head">
                <div>
                  <span class="eyebrow">{{ plan.premium ? 'Premium' : 'Free' }}</span>
                  <h2>{{ plan.displayName }}</h2>
                </div>
                <strong>{{ plan.amount ? (plan.currency + ' ' + plan.amount) : 'Free' }}</strong>
              </div>
              <p>{{ plan.durationDays >= 365 ? 'Long-term access' : plan.durationDays + ' days access' }}</p>
              <ul>
                @for (benefit of plan.benefits; track benefit) {
                  <li><span class="material-symbols-rounded">check_circle</span>{{ benefit }}</li>
                }
              </ul>
              <button class="primary-button" type="button" [disabled]="loadingPlan() === plan.planType" (click)="choose(plan)">
                {{ loadingPlan() === plan.planType ? 'Processing...' : plan.premium ? 'Upgrade plan' : 'Activate free plan' }}
              </button>
            </article>
          }
        </section>
      } @else {
        <app-empty-state icon="PY" title="No plans available" description="Payment plans could not be loaded right now." />
      }
    </section>
  `,
})
export class SubscriptionPageComponent {
  private readonly payments = inject(PaymentService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly plans = signal<SubscriptionPlanResponse[]>([]);
  protected readonly status = signal<SubscriptionStatusResponse | null>(null);
  protected readonly profileId = signal<number | null>(null);
  protected readonly profile = signal<ProfileResponse | null>(null);
  protected readonly loadingPlan = signal<string | null>(null);
  protected readonly role = computed<PaymentRole>(() => this.session.role() === 'RECRUITER' ? 'RECRUITER' : 'CANDIDATE');

  constructor() {
    this.load();
  }

  protected choose(plan: SubscriptionPlanResponse): void {
    const profileId = this.profileId();
    const profile = this.profile();
    const user = this.session.user();
    if (!profileId || !profile || !user) {
      this.toast.error('Profile required', 'Create your profile before choosing a subscription.');
      return;
    }
    if (!profile.mobile) {
      this.toast.error('Mobile required', 'Add a valid mobile number to your profile before payment.');
      return;
    }
    this.loadingPlan.set(plan.planType);
    this.payments.createOrder({
      userId: profileId,
      role: this.role(),
      planType: plan.planType,
      customerName: profile.fullName || user.fullName || user.email.split('@')[0],
      customerEmail: profile.email || user.email,
      customerPhone: String(profile.mobile),
    }).subscribe({
      next: (order) => {
        if (order.paymentStatus === 'SUCCESS') {
          this.toast.success('Plan activated', 'Your free subscription is active.');
          this.load();
        } else {
          window.sessionStorage.setItem('hireconnect.pendingPaymentOrder', JSON.stringify(order));
          this.toast.info('Redirecting to payment', 'Complete the checkout to activate premium.');
          this.router.navigate([`/${this.role().toLowerCase()}/subscription/checkout`]);
        }
        this.loadingPlan.set(null);
      },
      error: (error: unknown) => {
        this.loadingPlan.set(null);
        this.toast.error('Payment failed', getErrorMessage(error, 'Unable to create payment order.'));
      },
    });
  }

  protected cancel(): void {
    const profileId = this.profileId();
    if (!profileId) {
      return;
    }
    this.payments.cancel(profileId).subscribe({
      next: () => {
        this.toast.success('Subscription cancelled', 'Premium access has been cancelled.');
        this.load();
      },
      error: (error: unknown) => this.toast.error('Cancel failed', getErrorMessage(error, 'Unable to cancel subscription.')),
    });
  }

  protected freePlanLabel(): string {
    return this.role() === 'RECRUITER' ? 'RECRUITER_FREE' : 'CANDIDATE_FREE';
  }

  private load(): void {
    this.viewerProfile.getCurrentProfile(true).subscribe((profile) => {
      this.profile.set(profile);
      this.profileId.set(profile?.profileId ?? null);
      forkJoin({
        plans: this.payments.getPlans(this.role()).pipe(catchError(() => of([] as SubscriptionPlanResponse[]))),
        status: profile ? this.payments.getStatus(profile.profileId).pipe(catchError(() => of(null))) : of(null),
      }).subscribe(({ plans, status }) => {
        this.plans.set(plans);
        this.status.set(status);
      });
    });
  }
}
