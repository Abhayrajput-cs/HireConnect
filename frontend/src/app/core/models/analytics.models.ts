export interface AnalyticsSummary {
  totalJobs: number;
  totalApplications: number;
  shortlistedCount: number;
  offeredCount: number;
  rejectedCount: number;
  avgTimeToHireDays: number;
  viewToApplyRatio: number;
}

export interface RecruiterJobAnalytics {
  jobId: number;
  title: string;
  category: string;
  viewCount: number;
  applicationCount: number;
  ratio: number;
}
