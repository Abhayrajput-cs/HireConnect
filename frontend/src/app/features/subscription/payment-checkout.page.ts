import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CreateOrderResponse } from '../../core/models/payment.models';
import { PaymentService } from '../../core/services/payment.service';
import { SessionService } from '../../core/services/session.service';
import { ToastService } from '../../core/services/toast.service';
import { getErrorMessage } from '../../core/utils/http-error.util';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

interface RazorpayCheckoutResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

interface RazorpayCheckout {
  open: () => void;
  on: (eventName: 'payment.failed', handler: (response: unknown) => void) => void;
}

interface RazorpayFailureResponse {
  error?: {
    description?: string;
    reason?: string;
  };
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => RazorpayCheckout;
  }
}

@Component({
  selector: 'app-payment-checkout-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, PageHeaderComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Razorpay checkout"
        title="Complete payment to activate premium"
        description="A secure Razorpay payment window will open. Premium activates only after Razorpay payment verification succeeds."
      />

      @if (order(); as currentOrder) {
        <section class="payment-gateway-shell">
          <aside class="payment-gateway-summary">
            <div class="gateway-brand">
              <span class="brand-mark">HC</span>
              <div>
                <span class="eyebrow">HireConnect premium</span>
                <h2>{{ currentOrder.planType }}</h2>
              </div>
            </div>

            <div class="gateway-amount">
              <span>Total payable</span>
              <strong>{{ currentOrder.currency }} {{ currentOrder.amount }}</strong>
            </div>

            <dl class="gateway-order-list">
              <div>
                <dt>HireConnect order</dt>
                <dd>{{ currentOrder.orderId }}</dd>
              </div>
              <div>
                <dt>Razorpay order</dt>
                <dd>{{ currentOrder.gatewayOrderId || 'Not created' }}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{{ currentOrder.paymentStatus }}</dd>
              </div>
            </dl>

            <div class="gateway-secure-strip">
              <span class="material-symbols-rounded">verified_user</span>
              <span>Payment is processed by Razorpay. HireConnect verifies the signature before activating premium.</span>
            </div>
          </aside>

          <section class="payment-gateway-panel">
            <div class="gateway-panel-head">
              <div>
                <span class="eyebrow">Razorpay secure payment</span>
                <h2>Open payment window</h2>
                <p>Use Razorpay test mode cards, UPI, wallet, or net banking. Your subscription is not activated until this payment succeeds.</p>
              </div>
              <span class="gateway-badge">RAZORPAY</span>
            </div>

            <div class="razorpay-order-card">
              <span class="material-symbols-rounded">payments</span>
              <div>
                <strong>Payment order ready</strong>
                <p>{{ currentOrder.gatewayOrderId }}</p>
              </div>
            </div>

            <div class="gateway-actions">
              <button class="primary-button" type="button" [disabled]="processing()" (click)="openRazorpay(currentOrder)">
                {{ processing() ? 'Opening Razorpay...' : 'Pay with Razorpay' }}
                <span class="material-symbols-rounded">arrow_forward</span>
              </button>
              <a class="ghost-button" [routerLink]="['/', rolePath(), 'subscription']">Back to plans</a>
            </div>
          </section>
        </section>
      } @else {
        <app-empty-state icon="PY" title="No pending payment" description="Choose a premium plan first, then checkout will open here." />
        <a class="primary-button" [routerLink]="['/', rolePath(), 'subscription']">Open subscription plans</a>
      }
    </section>
  `,
})
export class PaymentCheckoutPageComponent implements AfterViewInit {
  private readonly payments = inject(PaymentService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);

  protected readonly order = signal<CreateOrderResponse | null>(this.readOrder());
  protected readonly processing = signal(false);

  ngAfterViewInit(): void {
    const currentOrder = this.order();
    if (currentOrder?.gatewayOrderId) {
      window.setTimeout(() => this.openRazorpay(currentOrder), 450);
    }
  }

  protected openRazorpay(order: CreateOrderResponse): void {
    if (!order.gatewayOrderId || !order.razorpayKeyId || !order.amountInPaise) {
      this.toast.error('Razorpay order missing', 'Create the premium order again. Razorpay order details are incomplete.');
      return;
    }

    this.processing.set(true);
    this.loadRazorpaySdk().then(() => {
      if (!window.Razorpay) {
        throw new Error('Razorpay SDK unavailable');
      }

      const checkout = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: 'HireConnect',
        description: order.planType,
        order_id: order.gatewayOrderId,
        prefill: {
          name: order.customerName,
          email: order.customerEmail,
          contact: order.customerPhone,
        },
        theme: { color: '#0ea5ff' },
        handler: (response: RazorpayCheckoutResponse) => this.verify(order, response),
        modal: {
          ondismiss: () => {
            this.processing.set(false);
            this.toast.info('Payment cancelled', 'Razorpay checkout was closed before payment completion.');
          },
        },
      });
      checkout.on('payment.failed', (response: unknown) => {
        this.processing.set(false);
        this.toast.error('Payment failed', this.razorpayFailureMessage(response));
      });
      checkout.open();
    }).catch(() => {
      this.processing.set(false);
      this.toast.error('Razorpay unavailable', 'Unable to open Razorpay checkout. Check internet access and Razorpay setup.');
    });
  }

  protected rolePath(): string {
    return this.session.role() === 'RECRUITER' ? 'recruiter' : 'candidate';
  }

  private verify(order: CreateOrderResponse, response: RazorpayCheckoutResponse): void {
    this.payments.verify({
      orderId: order.orderId,
      transactionId: response.razorpay_payment_id,
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature,
    }).subscribe({
      next: (transaction) => {
        this.processing.set(false);
        if (transaction.paymentStatus !== 'SUCCESS') {
          this.toast.error('Payment not verified', 'Razorpay payment could not be verified.');
          return;
        }
        window.sessionStorage.removeItem('hireconnect.pendingPaymentOrder');
        this.order.set(null);
        this.toast.success('Subscription active', 'Premium benefits are now enabled.');
        this.router.navigate([`/${this.rolePath()}/subscription`]);
      },
      error: (error: unknown) => {
        this.processing.set(false);
        window.sessionStorage.removeItem('hireconnect.pendingPaymentOrder');
        this.order.set(null);
        this.toast.error('Verification failed', getErrorMessage(error, 'Unable to verify this Razorpay payment.'));
      },
    });
  }

  private razorpayFailureMessage(response: unknown): string {
    const failure = response as RazorpayFailureResponse;
    const detail = failure?.error?.description || failure?.error?.reason;
    if (detail?.trim()) {
      return detail;
    }
    return 'Razorpay could not complete this payment. Please try again with a valid test card, UPI, or another payment method.';
  }

  private readOrder(): CreateOrderResponse | null {
    try {
      const raw = window.sessionStorage.getItem('hireconnect.pendingPaymentOrder');
      const order = raw ? JSON.parse(raw) as CreateOrderResponse : null;
      const currentEmail = this.session.user()?.email?.toLowerCase();
      if (order && currentEmail && order.customerEmail?.toLowerCase() !== currentEmail) {
        window.sessionStorage.removeItem('hireconnect.pendingPaymentOrder');
        return null;
      }
      return order;
    } catch {
      window.sessionStorage.removeItem('hireconnect.pendingPaymentOrder');
      return null;
    }
  }

  private loadRazorpaySdk(): Promise<void> {
    if (window.Razorpay) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>('script[data-razorpay-sdk="true"]');
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.dataset['razorpaySdk'] = 'true';
      script.onload = () => resolve();
      script.onerror = () => reject();
      document.body.appendChild(script);
    });
  }
}
