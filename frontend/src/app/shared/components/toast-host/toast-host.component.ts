import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-host">
      @for (toast of toastService.messages(); track toast.id) {
        <article class="toast-item" [class]="'toast-item toast-item--' + toast.tone">
          <div>
            <strong>{{ toast.title }}</strong>
            <p>{{ toast.message }}</p>
          </div>
          <button type="button" (click)="toastService.dismiss(toast.id)">Dismiss</button>
        </article>
      }
    </div>
  `,
  styles: [`
    .toast-host {
      position: fixed;
      right: 1.25rem;
      bottom: 1.25rem;
      z-index: 1200;
      display: grid;
      gap: 0.85rem;
      width: min(22rem, calc(100vw - 2rem));
    }
    .toast-item {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1.1rem;
      border-radius: 1rem;
      border: 1px solid rgba(148, 163, 184, 0.24);
      background: rgba(11, 18, 32, 0.95);
      color: #f8fafc;
      backdrop-filter: blur(14px);
    }
    .toast-item strong { display: block; margin-bottom: 0.25rem; font-size: 0.95rem; }
    .toast-item p { margin: 0; color: rgba(226, 232, 240, 0.86); font-size: 0.88rem; }
    .toast-item button { border: 0; background: transparent; color: inherit; opacity: 0.78; }
    .toast-item--success { border-color: rgba(16, 185, 129, 0.42); }
    .toast-item--error { border-color: rgba(239, 68, 68, 0.42); }
    .toast-item--info { border-color: rgba(14, 165, 233, 0.42); }
  `],
})
export class ToastHostComponent {
  protected readonly toastService = inject(ToastService);
}
