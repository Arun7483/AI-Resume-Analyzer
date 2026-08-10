import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, tap, throwError } from 'rxjs';
import { ResumeAnalysisResponse } from '../models/resume.model';

@Injectable({ providedIn: 'root' })
export class ResumeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/resumes';
  readonly analysis = signal<ResumeAnalysisResponse | null>(null);
  readonly status = signal<'idle' | 'uploading' | 'analyzing' | 'complete' | 'error'>('idle');
  readonly uploadProgress = signal(0);
  readonly error = signal<string | null>(null);
  readonly isProcessing = computed(() => this.status() === 'uploading' || this.status() === 'analyzing');

  uploadAndAnalyze(file: File): void {
    const body = new FormData();
    body.append('file', file);
    this.status.set('uploading');
    this.error.set(null);
    this.uploadProgress.set(0);

    this.http.post<ResumeAnalysisResponse>(`${this.apiUrl}/upload`, body).pipe(
      tap(() => this.status.set('analyzing')),
      map(response => {
        this.analysis.set(response);
        this.status.set('complete');
        this.uploadProgress.set(100);
        return response;
      }),
      catchError(err => {
        this.status.set('error');
        this.error.set(err.error?.message ?? 'Upload failed. Please try again.');
        return throwError(() => err);
      })
    ).subscribe({ error: () => {} });
  }

  reset(): void {
    this.analysis.set(null);
    this.status.set('idle');
    this.uploadProgress.set(0);
    this.error.set(null);
  }
}
