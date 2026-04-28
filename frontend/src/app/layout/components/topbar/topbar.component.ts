import { CommonModule } from '@angular/common';
import { Component, computed, inject, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { SessionService } from '../../../core/services/session.service';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle.component';
import { UserMenuComponent } from '../user-menu/user-menu.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink, UserMenuComponent, ThemeToggleComponent],
  template: `
    <header class="topbar">
      <div>
        <button type="button" class="ghost-button topbar__toggle" (click)="menuToggle.emit()">Menu</button>
        <span class="eyebrow">HireConnect workspace</span>
        <h2>{{ title() }}</h2>
      </div>

      <div class="topbar__actions">
        <app-theme-toggle />
        <a class="ghost-button" routerLink="/">Explore platform</a>
        <app-user-menu />
      </div>
    </header>
  `,
})
export class TopbarComponent {
  readonly menuToggle = output<void>();
  protected readonly session = inject(SessionService);
  private readonly router = inject(Router);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly title = computed(() => {
    const url = this.currentUrl();
    if (url.includes('/candidate/')) return 'Candidate workspace';
    if (url.includes('/recruiter/')) return 'Recruiter command center';
    if (url.includes('/admin/')) return 'Admin operations hub';
    return 'HireConnect portal';
  });
}
