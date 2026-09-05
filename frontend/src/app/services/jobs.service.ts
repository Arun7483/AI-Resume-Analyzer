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
        this.jobs.set(jobs);
        this.loading.set(false);
      },
      error: response => {
        this.jobs.set([]);
        this.error.set('Live listings are temporarily unavailable. Please try again in a moment.');
        this.loading.set(false);
      }
    });
  }
}
