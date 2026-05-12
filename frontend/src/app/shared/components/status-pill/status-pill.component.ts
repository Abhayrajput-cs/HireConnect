import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-pill" [class]="toneClass()">{{ label() }}</span>
  `,
})
export class StatusPillComponent {
  readonly label = input.required<string>();
  protected readonly toneClass = computed(() => `status-pill status-pill--${this.label().toLowerCase().replace(/[^a-z0-9]+/g, '-')}`);
}
