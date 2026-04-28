import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button type="button" class="ghost-button theme-toggle" (click)="theme.toggle()">
      <span class="material-symbols-rounded">{{ theme.isDark() ? 'light_mode' : 'dark_mode' }}</span>
      <span>{{ theme.isDark() ? 'Light mode' : 'Dark mode' }}</span>
    </button>
  `,
})
export class ThemeToggleComponent {
  protected readonly theme = inject(ThemeService);
}
