export type AnalysisStatus = 'idle' | 'uploading' | 'analyzing' | 'complete' | 'error';
export interface ResumeInsight { title: string; description: string; category: 'content' | 'format' | 'impact' | 'skills'; }
export interface KeywordMatch { keyword: string; matched: boolean; importance: 'high' | 'medium' | 'low'; }
export interface ResumeAnalysisResponse { id: string; fileName: string; candidateName: string; overallScore: number; atsMatchPercentage: number; summary: string; strengths: ResumeInsight[]; improvements: ResumeInsight[]; keywords: KeywordMatch[]; analyzedAt: string; }
export interface UploadResponse { resumeId: string; fileName: string; status: 'UPLOADED' | 'PROCESSING' | 'COMPLETED'; message: string; }
export interface UploadProgress { progress: number; stage: AnalysisStatus; }
