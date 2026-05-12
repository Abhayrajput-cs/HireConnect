import { Routes } from '@angular/router';

export const RECRUITER_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () => import('./dashboard/recruiter-dashboard.page').then((m) => m.RecruiterDashboardPageComponent) },
  { path: 'profile', loadComponent: () => import('../profile/recruiter-profile/recruiter-profile.page').then((m) => m.RecruiterProfilePageComponent) },
  { path: 'jobs', loadComponent: () => import('./manage-jobs/recruiter-manage-jobs.page').then((m) => m.RecruiterManageJobsPageComponent) },
  { path: 'jobs/new', loadComponent: () => import('./job-editor/recruiter-job-editor.page').then((m) => m.RecruiterJobEditorPageComponent) },
  { path: 'jobs/:jobId/edit', loadComponent: () => import('./job-editor/recruiter-job-editor.page').then((m) => m.RecruiterJobEditorPageComponent) },
  { path: 'jobs/:jobId/applicants', loadComponent: () => import('./applicants/recruiter-applicants.page').then((m) => m.RecruiterApplicantsPageComponent) },
  { path: 'interviews', loadComponent: () => import('./interviews/recruiter-interviews.page').then((m) => m.RecruiterInterviewsPageComponent) },
  { path: 'interviews/:interviewId/join', loadComponent: () => import('../interviews/interview-room/interview-room.page').then((m) => m.InterviewRoomPageComponent) },
  { path: 'analytics', loadComponent: () => import('./analytics/recruiter-analytics.page').then((m) => m.RecruiterAnalyticsPageComponent) },
  { path: 'notifications', loadComponent: () => import('./notifications/recruiter-notifications.page').then((m) => m.RecruiterNotificationsPageComponent) },
  { path: 'subscription', loadComponent: () => import('../subscription/subscription.page').then((m) => m.SubscriptionPageComponent) },
  { path: 'subscription/checkout', loadComponent: () => import('../subscription/payment-checkout.page').then((m) => m.PaymentCheckoutPageComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
];
