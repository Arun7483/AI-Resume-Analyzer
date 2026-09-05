import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BriefcaseBusiness, FileSearch, LogOut, UserRound, LucideAngularModule } from 'lucide-angular';
import { AnalysisDashboardComponent } from '../analysis-dashboard/analysis-dashboard.component';
import { AiBotComponent } from '../ai-bot/ai-bot.component';
import { ResumeUploadComponent } from '../resume-upload/resume-upload.component';
import { AuthService } from '../../services/auth.service';
import { ResumeAnalysisResponse } from '../../models/resume.model';
import { ResumeService } from '../../services/resume.service';
import { JobMatchesComponent } from '../job-matches/job-matches.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ResumeUploadComponent, AnalysisDashboardComponent, AiBotComponent, JobMatchesComponent],
  template: `
    <div class="min-h-screen bg-slate-50">
      <header class="sticky top-0 z-30 border-b bg-white/90 backdrop-blur-xl">
        <div class="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6">
          <a class="flex items-center gap-2.5 font-bold">
            <span class="grid size-9 place-items-center rounded-xl bg-brand-600 text-white">
              <lucide-icon [img]="FileSearch" [size]="20" />
            </span>
            Resume<span class="-ml-2.5 text-brand-600">Pulse</span>
          </a>
          <nav class="ml-10 hidden gap-1 md:flex">
            <button type="button" class="rounded-lg px-3 py-2 text-sm font-bold" [class.bg-brand-50]="activeView() === 'dashboard'" [class.text-brand-700]="activeView() === 'dashboard'" (click)="activeView.set('dashboard')">Dashboard</button>
            <button type="button" class="px-3 py-2 text-sm text-slate-500" (click)="showProfile()">Profile</button>
            <button type="button" class="rounded-lg px-3 py-2 text-sm font-bold" [class.bg-brand-50]="activeView() === 'jobs'" [class.text-brand-700]="activeView() === 'jobs'" (click)="showJobs()">Job matches</button>
          </nav>
          <div class="ml-3 flex gap-1 md:hidden">
            <button type="button" class="flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-bold" [class.bg-brand-50]="activeView() === 'dashboard'" [class.text-brand-700]="activeView() === 'dashboard'" (click)="activeView.set('dashboard')">Dashboard</button>
            <button type="button" class="flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-bold" [class.bg-brand-50]="activeView() === 'jobs'" [class.text-brand-700]="activeView() === 'jobs'" (click)="showJobs()"><lucide-icon [img]="BriefcaseBusiness" [size]="14" /> Jobs</button>
          </div>
          <div class="ml-auto flex items-center gap-2">
            <button type="button" class="flex items-center gap-2 rounded-xl border border-slate-200 px-2 py-1.5 text-left hover:border-brand-200 hover:bg-brand-50" (click)="showProfile()" aria-label="Open user profile">
              <span class="grid size-8 place-items-center rounded-full bg-brand-600 text-xs font-bold text-white">
                {{ (auth.fullName() || 'U').charAt(0).toUpperCase() }}
              </span>
              <span class="hidden sm:block"><span class="block text-sm font-bold text-slate-900">{{ auth.fullName() || 'Account' }}</span><span class="block text-xs text-slate-400">Profile</span></span>
            </button>
            <button type="button" class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:border-red-200 hover:bg-red-50 hover:text-red-600" (click)="logout()" aria-label="Log out">
              <lucide-icon [img]="LogOut" [size]="15" />
              <span class="hidden sm:inline">Log out</span>
            </button>
          </div>
        </div>
      </header>
      <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        @if (activeView() === 'jobs') {
          <app-job-matches />
        } @else if (activeView() === 'profile') {
          <section class="max-w-2xl">
            <p class="text-xs font-bold uppercase tracking-[.18em] text-brand-600">Account</p>
            <h1 class="mt-1 text-3xl font-bold text-slate-950">Your profile</h1>
            <div class="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div class="flex items-center gap-4">
                <span class="grid size-16 place-items-center rounded-full bg-brand-500 text-xl font-bold text-white">{{ (auth.fullName() || 'U').charAt(0).toUpperCase() }}</span>
                <div><h2 class="text-xl font-bold text-slate-950">{{ auth.fullName() || 'Account' }}</h2><p class="text-sm text-slate-500">{{ auth.email() || 'Email account' }}</p></div>
              </div>
              <dl class="mt-8 grid gap-4 sm:grid-cols-2">
                <div class="rounded-xl bg-slate-50 p-4"><dt class="text-xs font-bold uppercase text-slate-400">Account type</dt><dd class="mt-1 font-semibold text-slate-800">{{ auth.role().replace('ROLE_', '') }}</dd></div>
                <div class="rounded-xl bg-slate-50 p-4"><dt class="text-xs font-bold uppercase text-slate-400">Status</dt><dd class="mt-1 font-semibold text-emerald-600">Active</dd></div>
              </dl>
              <div class="mt-8 border-t border-slate-100 pt-6">
                <div class="flex items-center justify-between gap-3"><h3 class="font-bold text-slate-900">Uploaded resumes</h3><span class="text-xs font-semibold text-slate-400">{{ resumeService.history().length }} files</span></div>
                @if (resumeService.history().length) {
                  <div class="mt-3 divide-y divide-slate-100 rounded-xl border border-slate-200">
                    @for (resume of resumeService.history(); track resume.id) {
                      <div class="flex items-center justify-between gap-3 px-4 py-3"><div class="min-w-0"><p class="truncate text-sm font-semibold text-slate-800">{{ resume.fileName }}</p><p class="mt-1 text-xs text-slate-500">Uploaded {{ resume.uploadedAt | date:'medium' }}</p></div><span class="shrink-0 rounded-full bg-brand-50 px-2 py-1 text-xs font-bold text-brand-700">Resume {{ resume.id }}</span></div>
                    }
                  </div>
                } @else { <p class="mt-3 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">No resumes uploaded yet.</p> }
              </div>
              <button type="button" class="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-3 text-sm font-bold text-white hover:bg-red-700" (click)="logout()"><lucide-icon [img]="LogOut" [size]="16" /> Log out</button>
            </div>
          </section>
        } @else {
        <p class="text-sm font-semibold text-brand-600">{{ currentDate }}</p>
        <h1 class="mt-1 text-3xl font-bold tracking-tight sm:text-4xl">Make your next application count.</h1>
        <p class="mt-2 max-w-2xl text-sm text-slate-500">AI-powered feedback that helps your experience get noticed and pass every screening system.</p>
        <div class="mt-8 grid items-start gap-7 xl:grid-cols-[340px_1fr]">
          <aside class="xl:sticky xl:top-24">
            <app-resume-upload [progress]="progress()" (upload)="analyze($event)" />
            <div class="mt-4 rounded-2xl bg-brand-50 p-4">
              <p class="text-xs font-bold text-brand-700">Your files stay private</p>
              <p class="mt-1 text-xs text-slate-500">Encrypted in transit and automatically deleted after 30 days.</p>
            </div>
          </aside>
          <app-analysis-dashboard [analysis]="analysis()" />
        </div>
        }
      </main>
      <app-ai-bot [resumeId]="currentResumeId()" />
    </div>
  `
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly resumeService = inject(ResumeService);
  readonly FileSearch = FileSearch;
  readonly BriefcaseBusiness = BriefcaseBusiness;
  readonly LogOut = LogOut;
  readonly UserRound = UserRound;
  
  readonly progress = signal(0);
  readonly analysis = signal<ResumeAnalysisResponse | null>(null);
  readonly currentResumeId = signal<number | null>(null);
  readonly activeView = signal<'dashboard' | 'jobs' | 'profile'>('dashboard');
  currentDate = '';

  ngOnInit(): void {
    this.updateCurrentDate();
    this.resumeService.loadHistory();
    setInterval(() => this.updateCurrentDate(), 60000); // Update every minute
  }

  private updateCurrentDate(): void {
    const options: Intl.DateTimeFormatOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    this.currentDate = new Date().toLocaleDateString('en-US', options);
  }

  analyze(file: File): void {
    this.progress.set(8);
    const timer = setInterval(() => {
      this.progress.update(value => Math.min(value + 12, 90));
    }, 220);

    this.resumeService.uploadResume(file).subscribe({
      next: (response: ResumeAnalysisResponse) => {
        clearInterval(timer);
        this.progress.set(100);
        this.analysis.set(response);
        this.currentResumeId.set(parseInt(response.id, 10));
        setTimeout(() => this.progress.set(0), 1000);
      },
      error: (error: any) => {
        clearInterval(timer);
        console.error('Resume analysis failed:', error);
        this.progress.set(0);
        alert(error.error?.message ?? 'Failed to analyze resume. Please try again.');
      }
    });
  }

  showJobs(): void {
    this.activeView.set('jobs');
  }

  showProfile(): void {
    this.activeView.set('profile');
  }

  logout(): void {
    this.auth.logout();
    window.location.reload();
  }
}
