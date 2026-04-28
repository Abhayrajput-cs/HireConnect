import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="dashboard-shell">
      <app-sidebar [open]="sidebarOpen()" />
      <div class="dashboard-shell__content">
        <app-topbar (menuToggle)="sidebarOpen.update((value) => !value)" />
        <main class="dashboard-shell__canvas">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class DashboardLayoutComponent {
  protected readonly sidebarOpen = signal(false);
}
