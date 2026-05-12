import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="stat-card">
      <span class="stat-card__icon material-symbols-rounded">{{ resolvedIcon() }}</span>
      <div>
        <p class="stat-card__label">{{ label() }}</p>
        <h3>{{ value() }}</h3>
        @if (caption()) {
          <small>{{ caption() }}</small>
        }
      </div>
    </article>
  `,
})
export class StatCardComponent {
  readonly icon = input('HC');
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly caption = input<string>('');

  protected resolvedIcon(): string {
    return ICON_MAP[this.icon()] ?? this.icon();
  }
}

const ICON_MAP: Record<string, string> = {
  HC: 'hub',
  JB: 'work',
  AP: 'description',
  IV: 'calendar_month',
  NT: 'notifications',
  PF: 'badge',
  AN: 'insights',
  SL: 'award_star',
  TT: 'timer',
  US: 'groups',
  SV: 'bookmark',
};
