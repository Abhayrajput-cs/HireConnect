import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page-header">
      <div>
        <span class="eyebrow">{{ eyebrow() }}</span>
        <h1>{{ title() }}</h1>
        @if (description()) {
          <p>{{ description() }}</p>
        }
      </div>
      @if (actionLabel() && actionLink()) {
        <a class="ghost-button" [routerLink]="actionLink()">{{ actionLabel() }}</a>
      }
    </section>
  `,
})
export class PageHeaderComponent {
  readonly eyebrow = input('HireConnect Workspace');
  readonly title = input.required<string>();
  readonly description = input<string>('');
  readonly actionLabel = input<string>('');
  readonly actionLink = input<string>('');
}
