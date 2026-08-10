import { HttpClient, HttpEventType } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, filter, map, Observable, tap, throwError } from 'rxjs';
import { ResumeAnalysisResponse, UploadResponse } from '../models/resume.model';
@Injectable({ providedIn: 'root' })
export class ResumeService {
  private readonly http = inject(HttpClient); private readonly apiUrl = 'http://localhost:8080/api/v1/resumes';
  readonly analysis = signal<ResumeAnalysisResponse | null>(null); readonly status = signal<'idle'|'uploading'|'analyzing'|'complete'|'error'>('idle'); readonly uploadProgress = signal(0); readonly error = signal<string|null>(null);
  readonly isProcessing = computed(() => this.status() === 'uploading' || this.status() === 'analyzing');
  uploadResume(file: File): Observable<UploadResponse> { const body = new FormData(); body.append('file', file); this.status.set('uploading'); this.error.set(null); this.uploadProgress.set(0);
    return this.http.post<UploadResponse>(`${this.apiUrl}/upload`, body, { observe:'events', reportProgress:true }).pipe(tap(event => { if (event.type === HttpEventType.UploadProgress) this.uploadProgress.set(Math.round(100 * event.loaded / (event.total ?? event.loaded))); }), filter(event => event.type === HttpEventType.Response), map(event => { this.status.set('analyzing'); return event.body as UploadResponse; }), catchError(err => { this.status.set('error'); this.error.set(err.error?.message ?? 'Upload failed. Please try again.'); return throwError(() => err); })); }
  getAnalysis(resumeId: string): Observable<ResumeAnalysisResponse> { return this.http.get<ResumeAnalysisResponse>(`${this.apiUrl}/${resumeId}/analysis`).pipe(tap(result => { this.analysis.set(result); this.status.set('complete'); }), catchError(err => { this.status.set('error'); this.error.set('We could not analyze this resume.'); return throwError(() => err); })); }
  reset(): void { this.analysis.set(null); this.status.set('idle'); this.uploadProgress.set(0); this.error.set(null); }
}
