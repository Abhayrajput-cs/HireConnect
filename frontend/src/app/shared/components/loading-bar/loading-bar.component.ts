import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loading-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loading.isLoading()) {
      <div class="loading-bar">
        <span class="loading-bar__beam"></span>
      </div>
    }
  `,
  styles: [`
    .loading-bar {
      position: fixed;
      inset: 0 0 auto;
      z-index: 1100;
      height: 3px;
      background: transparent;
      overflow: hidden;
    }
    .loading-bar__beam {
      display: block;
      width: 32%;
      height: 100%;
      border-radius: 999px;
      background: linear-gradient(90deg, #20c997 0%, #0ea5e9 45%, #f59e0b 100%);
      box-shadow: 0 0 18px rgba(14, 165, 233, 0.45);
      animation: beam 1.4s ease-in-out infinite;
    }
    @keyframes beam {
      0% { transform: translateX(-120%); }
      100% { transform: translateX(430%); }
    }
  `],
})
export class LoadingBarComponent {
  protected readonly loading = inject(LoadingService);
}
