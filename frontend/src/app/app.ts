import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LoadingBarComponent } from './shared/components/loading-bar/loading-bar.component';
import { ToastHostComponent } from './shared/components/toast-host/toast-host.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, LoadingBarComponent, ToastHostComponent],
  template: `
    <app-loading-bar />
    <router-outlet />
    <app-toast-host />
  `,
})
export class App {}
