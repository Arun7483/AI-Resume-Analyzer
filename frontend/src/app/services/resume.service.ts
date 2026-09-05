import { HttpClient, HttpEventType } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, filter, map, Observable, tap, throwError } from 'rxjs';
import { ResumeAnalysisResponse, ResumeHistory, UploadResponse } from '../models/resume.model';
import { API_BASE_URL } from '../api.config';
@Injectable({ providedIn: 'root' })
export class ResumeService {
  private readonly http = inject(HttpClient); private readonly apiUrl = `${API_BASE_URL}/api/v1/resumes`;
  readonly analysis = signal<ResumeAnalysisResponse | null>(null); readonly status = signal<'idle'|'uploading'|'analyzing'|'complete'|'error'>('idle'); readonly uploadProgress = signal(0); readonly error = signal<string|null>(null); readonly history = signal<ResumeHistory[]>([]);
  readonly isProcessing = computed(() => this.status() === 'uploading' || this.status() === 'analyzing');
  uploadResume(file: File): Observable<ResumeAnalysisResponse> { const body = new FormData(); body.append('file', file); this.status.set('uploading'); this.error.set(null); this.uploadProgress.set(0);
    return this.http.post<UploadResponse>(`${this.apiUrl}/upload`, body, { observe:'events', reportProgress:true }).pipe(tap(event => { if (event.type === HttpEventType.UploadProgress) this.uploadProgress.set(Math.round(100 * event.loaded / (event.total ?? event.loaded))); }), filter(event => event.type === HttpEventType.Response), map(event => { this.status.set('complete'); const result = this.toDisplayAnalysis(event.body as UploadResponse); this.analysis.set(result); this.loadHistory(); return result; }), catchError(err => { this.status.set('error'); this.error.set(err.error?.message ?? 'Upload and analysis failed. Please try again.'); return throwError(() => err); })); }
  loadHistory(): void { this.http.get<ResumeHistory[]>(`${this.apiUrl}/history`).subscribe({ next: history => this.history.set(history), error: () => this.history.set([]) }); }
  private toDisplayAnalysis(result: UploadResponse): ResumeAnalysisResponse { return { id: String(result.resumeId), fileName: result.fileName, candidateName: 'Your resume', overallScore: result.overallScore, atsMatchPercentage: result.atsMatchPercentage, summary: '', strengths: result.strengths.map((description, index) => ({ title: `Strength ${index + 1}`, description, category: 'impact' })), improvements: result.weaknesses.map((description, index) => ({ title: `Improvement ${index + 1}`, description, category: 'content' })), keywords: result.missingKeywords.map(keyword => ({ keyword, matched: false, importance: 'medium' })), analyzedAt: result.analyzedAt }; }
  reset(): void { this.analysis.set(null); this.status.set('idle'); this.uploadProgress.set(0); this.error.set(null); }
}
