import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () => import('./dashboard/admin-dashboard.page').then((m) => m.AdminDashboardPageComponent) },
  { path: 'users', loadComponent: () => import('./users/admin-users.page').then((m) => m.AdminUsersPageComponent) },
  { path: 'jobs', loadComponent: () => import('./jobs/admin-jobs.page').then((m) => m.AdminJobsPageComponent) },
  { path: 'analytics', loadComponent: () => import('./analytics/admin-analytics.page').then((m) => m.AdminAnalyticsPageComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
];
