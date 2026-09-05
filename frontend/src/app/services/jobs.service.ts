import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { API_BASE_URL } from '../api.config';
import { JobMatch } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobsService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${API_BASE_URL}/api/v1/resumes/job-matches`;

  readonly jobs = signal<JobMatch[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  loadMatches(): void {
    this.loading.set(true);
    this.error.set('');
    this.http.get<JobMatch[]>(this.endpoint).subscribe({
      next: jobs => {
        this.jobs.set(jobs.length ? jobs : this.fallbackJobs);
        this.loading.set(false);
      },
      error: response => {
        this.jobs.set(this.fallbackJobs);
        this.error.set('Live listings are temporarily unavailable. Showing active search links instead.');
        this.loading.set(false);
      }
    });
  }

  private readonly fallbackJobs: JobMatch[] = [
    {
      title: 'Software Engineer',
      company: 'LinkedIn job search',
      location: 'Remote',
      description: 'Browse currently active software engineering roles and apply manually on LinkedIn.',
      applyUrl: 'https://www.linkedin.com/jobs/search/?keywords=software%20engineer',
      matchPercentage: 50,
      remote: true
    },
    {
      title: 'Frontend Developer',
      company: 'LinkedIn job search',
      location: 'Remote',
      description: 'Browse currently active frontend developer roles and apply manually on LinkedIn.',
      applyUrl: 'https://www.linkedin.com/jobs/search/?keywords=frontend%20developer',
      matchPercentage: 50,
      remote: true
    },
    {
      title: 'Backend Developer',
      company: 'LinkedIn job search',
      location: 'Remote',
      description: 'Browse currently active backend developer roles and apply manually on LinkedIn.',
      applyUrl: 'https://www.linkedin.com/jobs/search/?keywords=backend%20developer',
      matchPercentage: 50,
      remote: true
    },
    {
      title: 'Data Analyst',
      company: 'LinkedIn job search',
      location: 'Remote',
      description: 'Browse currently active data analyst roles and apply manually on LinkedIn.',
      applyUrl: 'https://www.linkedin.com/jobs/search/?keywords=data%20analyst',
      matchPercentage: 50,
      remote: true
    }
  ];
}
