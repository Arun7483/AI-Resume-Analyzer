import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { API_BASE_URL } from '../api.config';
import { JobMatch, JobMatchPage } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobsService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${API_BASE_URL}/api/v1/resumes/job-matches`;

  readonly jobs = signal<JobMatch[]>([]);
  readonly total = signal(0);
  readonly hasMore = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');

  loadMatches(page = 0, append = false): void {
    this.loading.set(true);
    this.error.set('');
    this.http.get<JobMatchPage>(this.endpoint, { params: { page, size: 24 } }).subscribe({
      next: result => {
        this.jobs.set(append ? [...this.jobs(), ...result.jobs] : result.jobs);
        this.total.set(result.total);
        this.hasMore.set(result.hasMore);
        this.loading.set(false);
      },
      error: response => {
        this.jobs.set([]);
        this.total.set(0);
        this.hasMore.set(false);
        this.error.set(response.error?.message ?? 'Live listings are temporarily unavailable. Please try again in a moment.');
        this.loading.set(false);
      }
    });
  }
}
