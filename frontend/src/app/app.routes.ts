import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';
import { DashboardLayoutComponent } from './layout/dashboard-layout.component';
import { PublicLayoutComponent } from './layout/public-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/public/landing/landing.page').then((m) => m.LandingPageComponent),
      },
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login/login.page').then((m) => m.LoginPageComponent),
      },
      {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/register/register.page').then((m) => m.RegisterPageComponent),
      },
      {
        path: 'oauth2/callback',
        loadComponent: () => import('./features/auth/oauth-callback/oauth-callback.page').then((m) => m.OAuthCallbackPageComponent),
      },
      {
        path: 'unauthorized',
        loadComponent: () => import('./features/public/unauthorized/unauthorized.page').then((m) => m.UnauthorizedPageComponent),
      },
      {
        path: 'dashboard',
        canActivate: [authGuard],
        loadComponent: () => import('./features/public/role-home/role-home.page').then((m) => m.RoleHomePageComponent),
      },
    ],
  },
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'candidate',
        canActivate: [roleGuard],
        data: { roles: ['CANDIDATE'] },
        loadChildren: () => import('./features/candidate/candidate.routes').then((m) => m.CANDIDATE_ROUTES),
      },
      {
        path: 'recruiter',
        canActivate: [roleGuard],
        data: { roles: ['RECRUITER'] },
        loadChildren: () => import('./features/recruiter/recruiter.routes').then((m) => m.RECRUITER_ROUTES),
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () => import('./features/public/not-found/not-found.page').then((m) => m.NotFoundPageComponent),
  },
];
