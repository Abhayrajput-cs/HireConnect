export interface JobRequest {
  title: string;
  category: string;
  type: string;
  location: string;
  salaryMin: number;
  salaryMax: number;
  description: string;
  skills: string[];
  experienceRequired: number;
  postedBy: number;
  status?: string | null;
  postedAt?: string | null;
}

export interface JobResponse {
  jobId: number;
  title: string;
  category: string;
  type: string;
  location: string;
  salaryMin: number;
  salaryMax: number;
  description: string;
  skills: string[];
  experienceRequired: number;
  postedBy: number;
  companyName?: string | null;
  status: string;
  postedAt: string;
}

export interface JobQuery {
  title?: string | null;
  category?: string | null;
  location?: string | null;
  salaryMin?: number | null;
  salaryMax?: number | null;
  experienceRequired?: number | null;
  status?: string | null;
  postedBy?: number | null;
}
