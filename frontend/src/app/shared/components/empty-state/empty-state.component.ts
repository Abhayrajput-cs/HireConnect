import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="empty-state card-shell">
      <div class="empty-state__orb"></div>
      <span class="empty-state__icon material-symbols-rounded">{{ resolvedIcon() }}</span>
      <h3>{{ title() }}</h3>
      <p>{{ description() }}</p>
      @if (actionLabel() && actionLink()) {
        <a class="primary-button" [routerLink]="actionLink()">{{ actionLabel() }}</a>
      }
    </section>
  `,
})
export class EmptyStateComponent {
  readonly icon = input('HC');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly actionLabel = input<string>('');
  readonly actionLink = input<string>('');

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
