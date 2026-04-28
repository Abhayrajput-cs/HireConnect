import { Routes } from '@angular/router';

export const CANDIDATE_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () => import('./dashboard/candidate-dashboard.page').then((m) => m.CandidateDashboardPageComponent) },
  { path: 'profile', loadComponent: () => import('../profile/candidate-profile/candidate-profile.page').then((m) => m.CandidateProfilePageComponent) },
  { path: 'jobs', loadComponent: () => import('./jobs/candidate-jobs.page').then((m) => m.CandidateJobsPageComponent) },
  { path: 'jobs/:jobId', loadComponent: () => import('./job-detail/candidate-job-detail.page').then((m) => m.CandidateJobDetailPageComponent) },
  { path: 'applications', loadComponent: () => import('./applications/candidate-applications.page').then((m) => m.CandidateApplicationsPageComponent) },
  { path: 'interviews', loadComponent: () => import('./interviews/candidate-interviews.page').then((m) => m.CandidateInterviewsPageComponent) },
  { path: 'notifications', loadComponent: () => import('./notifications/candidate-notifications.page').then((m) => m.CandidateNotificationsPageComponent) },
  { path: 'bookmarks', loadComponent: () => import('./bookmarks/candidate-bookmarks.page').then((m) => m.CandidateBookmarksPageComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
];
