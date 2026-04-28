import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import { AnalyticsService } from './analytics.service';
import { JobService } from './job.service';
import { ProfileService } from './profile.service';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly profiles = inject(ProfileService);
  private readonly jobs = inject(JobService);
  private readonly analytics = inject(AnalyticsService);

  getDashboardData() {
    return forkJoin({
      users: this.profiles.getProfiles(),
      jobs: this.jobs.getJobs(),
      analytics: this.analytics.getPlatformStats(),
      topCategories: this.analytics.getTopCategories(),
    });
  }

  getUsersOverview(): Observable<{
    total: number;
    candidates: number;
    recruiters: number;
    admins: number;
  }> {
    return this.profiles.getProfiles().pipe(
      map((profiles) => ({
        total: profiles.length,
        candidates: profiles.filter((profile) => profile.role === 'CANDIDATE').length,
        recruiters: profiles.filter((profile) => profile.role === 'RECRUITER').length,
        admins: profiles.filter((profile) => profile.role === 'ADMIN').length,
      })),
    );
  }
}
