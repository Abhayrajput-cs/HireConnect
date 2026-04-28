import { Injectable, inject } from '@angular/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { ActivityItem } from '../models/activity.models';
import { ApplicationResponse } from '../models/application.models';
import { InterviewResponse } from '../models/interview.models';
import { JobResponse } from '../models/job.models';
import { NotificationResponse } from '../models/notification.models';
import { ApplicationService } from './application.service';
import { InterviewService } from './interview.service';
import { JobService } from './job.service';
import { NotificationService } from './notification.service';

type Scope = 'candidate' | 'recruiter';

@Injectable({ providedIn: 'root' })
export class ActivityFeedService {
  private readonly notifications = inject(NotificationService);
  private readonly applications = inject(ApplicationService);
  private readonly interviews = inject(InterviewService);
  private readonly jobs = inject(JobService);

  getCandidateFeed(profileId: number) {
    return forkJoin({
      notifications: this.notifications.getByUser(profileId).pipe(catchError(() => of([] as NotificationResponse[]))),
      applications: this.applications.getByCandidate(profileId).pipe(catchError(() => of([] as ApplicationResponse[]))),
    }).pipe(
      switchMap(({ notifications, applications }) => {
        if (!applications.length) {
          return of(this.decorate(profileId, 'candidate', [
            ...this.mapNotificationItems(notifications),
          ]));
        }

        return forkJoin({
          interviews: forkJoin(
            applications.map((application) =>
              this.interviews.getByApplication(application.applicationId).pipe(catchError(() => of([] as InterviewResponse[]))),
            ),
          ),
          jobs: this.loadJobs(applications.map((application) => application.jobId)),
        }).pipe(
          map(({ interviews, jobs }) =>
            this.decorate(profileId, 'candidate', [
              ...this.mapNotificationItems(notifications),
              ...this.mapCandidateDerivedItems(applications, interviews.flat(), jobs),
            ]),
          ),
        );
      }),
    );
  }

  getRecruiterFeed(profileId: number) {
    return forkJoin({
      notifications: this.notifications.getByUser(profileId).pipe(catchError(() => of([] as NotificationResponse[]))),
      jobs: this.jobs.getJobsByRecruiter(profileId).pipe(catchError(() => of([] as JobResponse[]))),
    }).pipe(
      switchMap(({ notifications, jobs }) => {
        if (!jobs.length) {
          return of(this.decorate(profileId, 'recruiter', [
            ...this.mapNotificationItems(notifications),
          ]));
        }

        return forkJoin({
          applications: forkJoin(
            jobs.map((job) => this.applications.getByJob(job.jobId).pipe(catchError(() => of([] as ApplicationResponse[])))),
          ),
        }).pipe(
          switchMap(({ applications }) => {
            const flatApplications = applications.flat();
            if (!flatApplications.length) {
              return of(
                this.decorate(profileId, 'recruiter', [
                  ...this.mapNotificationItems(notifications),
                  ...this.mapRecruiterDerivedItems(jobs, [], []),
                ]),
              );
            }

            return forkJoin({
              interviews: forkJoin(
                flatApplications.map((application) =>
                  this.interviews.getByApplication(application.applicationId).pipe(catchError(() => of([] as InterviewResponse[]))),
                ),
              ),
            }).pipe(
              map(({ interviews }) =>
                this.decorate(profileId, 'recruiter', [
                  ...this.mapNotificationItems(notifications),
                  ...this.mapRecruiterDerivedItems(jobs, flatApplications, interviews.flat()),
                ]),
              ),
            );
          }),
        );
      }),
    );
  }

  markDerivedAsRead(scope: Scope, profileId: number, itemId: string): void {
    const set = this.getSet(this.readKey(scope, profileId));
    set.add(itemId);
    this.persistSet(this.readKey(scope, profileId), set);
  }

  markAllDerivedAsRead(scope: Scope, profileId: number, itemIds: string[]): void {
    const set = this.getSet(this.readKey(scope, profileId));
    itemIds.forEach((itemId) => set.add(itemId));
    this.persistSet(this.readKey(scope, profileId), set);
  }

  dismissDerived(scope: Scope, profileId: number, itemId: string): void {
    const set = this.getSet(this.dismissedKey(scope, profileId));
    set.add(itemId);
    this.persistSet(this.dismissedKey(scope, profileId), set);
  }

  private loadJobs(jobIds: number[]) {
    const uniqueIds = [...new Set(jobIds)];
    if (!uniqueIds.length) {
      return of([] as JobResponse[]);
    }
    return forkJoin(
      uniqueIds.map((jobId) => this.jobs.getJob(jobId).pipe(catchError(() => of(null)))),
    ).pipe(
      map((jobs) => jobs.filter((job): job is JobResponse => !!job)),
    );
  }

  private decorate(profileId: number, scope: Scope, items: ActivityItem[]): ActivityItem[] {
    const dismissed = this.getSet(this.dismissedKey(scope, profileId));
    const read = this.getSet(this.readKey(scope, profileId));

    return items
      .filter((item) => !dismissed.has(item.id))
      .map((item) =>
        item.source === 'derived'
          ? { ...item, isRead: item.isRead || read.has(item.id) }
          : item,
      )
      .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime());
  }

  private mapNotificationItems(notifications: NotificationResponse[]): ActivityItem[] {
    return notifications.map((notification) => ({
      id: `notification-${notification.notificationId}`,
      source: 'notification',
      notificationId: notification.notificationId,
      type: notification.type || 'SYSTEM',
      title: this.notificationTitle(notification),
      message: notification.message,
      status: notification.isRead ? 'READ' : 'UNREAD',
      createdAt: notification.createdAt,
      isRead: notification.isRead,
    }));
  }

  private mapCandidateDerivedItems(
    applications: ApplicationResponse[],
    interviews: InterviewResponse[],
    jobs: JobResponse[],
  ): ActivityItem[] {
    const jobsById = new Map(jobs.map((job) => [job.jobId, job]));
    const applicationById = new Map(applications.map((application) => [application.applicationId, application]));

    return [
      ...applications.map((application) => {
        const job = jobsById.get(application.jobId);
        return {
          id: `candidate-application-${application.applicationId}-${application.status}`,
          source: 'derived' as const,
          type: 'APPLICATION',
          title: job ? `${job.title} application updated` : `Application #${application.applicationId} updated`,
          message: this.applicationMessage(application.status, job?.title),
          status: application.status,
          createdAt: application.appliedAt,
          isRead: application.status === 'APPLIED',
          actionLabel: 'Open applications',
          actionLink: '/candidate/applications',
        };
      }),
      ...interviews.map((interview) => {
        const application = applicationById.get(interview.applicationId);
        const job = application ? jobsById.get(application.jobId) : null;
        return {
          id: `candidate-interview-${interview.interviewId}-${interview.status}-${interview.scheduledAt}`,
          source: 'derived' as const,
          type: 'INTERVIEW',
          title: job ? `${job.title} interview signal` : `Interview #${interview.interviewId}`,
          message: this.interviewMessage(interview, true),
          status: interview.status,
          createdAt: interview.scheduledAt,
          isRead: false,
          actionLabel: 'View interviews',
          actionLink: '/candidate/interviews',
        };
      }),
    ];
  }

  private mapRecruiterDerivedItems(
    jobs: JobResponse[],
    applications: ApplicationResponse[],
    interviews: InterviewResponse[],
  ): ActivityItem[] {
    const jobsById = new Map(jobs.map((job) => [job.jobId, job]));
    const applicationsById = new Map(applications.map((application) => [application.applicationId, application]));

    return [
      ...applications.map((application) => {
        const job = jobsById.get(application.jobId);
        return {
          id: `recruiter-application-${application.applicationId}-${application.status}`,
          source: 'derived' as const,
          type: 'PIPELINE',
          title: job ? `Applicant update for ${job.title}` : `Application #${application.applicationId}`,
          message: this.recruiterApplicationMessage(application.status, job?.title),
          status: application.status,
          createdAt: application.appliedAt,
          isRead: application.status === 'APPLIED',
          actionLabel: 'View applicants',
          actionLink: job ? `/recruiter/jobs/${job.jobId}/applicants` : '/recruiter/jobs',
        };
      }),
      ...interviews.map((interview) => {
        const application = applicationsById.get(interview.applicationId);
        const job = application ? jobsById.get(application.jobId) : null;
        return {
          id: `recruiter-interview-${interview.interviewId}-${interview.status}-${interview.scheduledAt}`,
          source: 'derived' as const,
          type: 'INTERVIEW',
          title: job ? `${job.title} interview activity` : `Interview #${interview.interviewId}`,
          message: this.interviewMessage(interview, false),
          status: interview.status,
          createdAt: interview.scheduledAt,
          isRead: false,
          actionLabel: 'Manage interviews',
          actionLink: job ? `/recruiter/jobs/${job.jobId}/applicants` : '/recruiter/jobs',
        };
      }),
    ];
  }

  private applicationMessage(status: string, jobTitle?: string): string {
    switch (status) {
      case 'SHORTLISTED':
        return `You were shortlisted${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'INTERVIEW_SCHEDULED':
        return `An interview has been lined up${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'OFFERED':
        return `Great news. An offer has been issued${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'REJECTED':
        return `The application${jobTitle ? ` for ${jobTitle}` : ''} was closed without moving forward.`;
      case 'WITHDRAWN':
        return `You withdrew this application${jobTitle ? ` for ${jobTitle}` : ''}.`;
      default:
        return `Application received${jobTitle ? ` for ${jobTitle}` : ''}.`;
    }
  }

  private recruiterApplicationMessage(status: string, jobTitle?: string): string {
    switch (status) {
      case 'SHORTLISTED':
        return `A candidate has been shortlisted${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'INTERVIEW_SCHEDULED':
        return `Interview scheduling has started${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'OFFERED':
        return `An offer has been sent${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'REJECTED':
        return `A candidate was rejected${jobTitle ? ` for ${jobTitle}` : ''}.`;
      case 'WITHDRAWN':
        return `A candidate withdrew${jobTitle ? ` from ${jobTitle}` : ''}.`;
      default:
        return `A new application has arrived${jobTitle ? ` for ${jobTitle}` : ''}.`;
    }
  }

  private interviewMessage(interview: InterviewResponse, candidateView: boolean): string {
    switch (interview.status) {
      case 'SCHEDULED':
        return candidateView
          ? `Interview scheduled for ${this.formatDate(interview.scheduledAt)}.`
          : `Interview slot allocated for ${this.formatDate(interview.scheduledAt)}.`;
      case 'CONFIRMED':
        return candidateView
          ? 'You confirmed your interview slot.'
          : 'The candidate confirmed the interview slot.';
      case 'RESCHEDULE_REQUESTED':
        return candidateView
          ? 'Your reschedule request has been sent to the recruiter.'
          : 'The candidate requested a new interview slot.';
      case 'CANCELED':
        return candidateView
          ? 'This interview was canceled by the recruiter.'
          : 'This interview was canceled.';
      default:
        return `Interview status changed to ${interview.status}.`;
    }
  }

  private notificationTitle(notification: NotificationResponse): string {
    const type = (notification.type || 'SYSTEM').replace(/_/g, ' ').toLowerCase();
    return type.replace(/\b\w/g, (character) => character.toUpperCase());
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('en-IN', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
        });
  }

  private readKey(scope: Scope, profileId: number): string {
    return `hireconnect.activity.read.${scope}.${profileId}`;
  }

  private dismissedKey(scope: Scope, profileId: number): string {
    return `hireconnect.activity.dismissed.${scope}.${profileId}`;
  }

  private getSet(key: string): Set<string> {
    if (typeof window === 'undefined') {
      return new Set<string>();
    }

    try {
      const parsed = JSON.parse(window.localStorage.getItem(key) ?? '[]') as string[];
      return new Set(parsed);
    } catch {
      return new Set<string>();
    }
  }

  private persistSet(key: string, values: Set<string>): void {
    if (typeof window === 'undefined') {
      return;
    }
    window.localStorage.setItem(key, JSON.stringify([...values]));
  }
}
