export interface JobMatch {
  jobId: string;
  title: string;
  company: string;
  location: string;
  country: string;
  description: string;
  applyUrl: string;
  publisher: string;
  category: string;
  postedAt: string;
  employmentType: string;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  matchPercentage: number;
  remote: boolean;
  matchedSkills: string[];
  missingSkills: string[];
}

export interface JobMatchPage {
  jobs: JobMatch[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}
