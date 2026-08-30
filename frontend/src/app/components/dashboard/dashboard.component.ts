import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Bell, ChevronDown, FileSearch, HelpCircle, LucideAngularModule } from 'lucide-angular';
import { AnalysisDashboardComponent } from '../analysis-dashboard/analysis-dashboard.component';
import { AiBotComponent } from '../ai-bot/ai-bot.component';
import { ResumeUploadComponent } from '../resume-upload/resume-upload.component';
import { AuthService } from '../../services/auth.service';
import { ResumeAnalysisResponse } from '../../models/resume.model';
import { ResumeService } from '../../services/resume.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ResumeUploadComponent, AnalysisDashboardComponent, AiBotComponent],
  template: `
    <div class="min-h-screen bg-slate-50">
      <header class="sticky top-0 z-30 border-b bg-white/90 backdrop-blur-xl">
        <div class="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6">
          <a class="flex items-center gap-2.5 font-bold">
            <span class="grid size-9 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-mint text-white">
              <lucide-icon [img]="FileSearch" [size]="20" />
            </span>
            Resume<span class="-ml-2.5 text-brand-600">Pulse</span>
          </a>
          <nav class="ml-10 hidden gap-1 md:flex">
            <a class="rounded-lg bg-brand-50 px-3 py-2 text-sm font-bold text-brand-700">Dashboard</a>
            <a class="px-3 py-2 text-sm text-slate-500">My resumes</a>
            <a class="px-3 py-2 text-sm text-slate-500">Job matches</a>
          </nav>
          <div class="ml-auto flex items-center gap-2">
            <lucide-icon [img]="HelpCircle" class="text-slate-400" />
            <lucide-icon [img]="Bell" class="text-slate-400" />
            <span class="ml-2 grid size-8 place-items-center rounded-full bg-brand-500 text-xs font-bold text-white">
              {{ (auth.fullName() || 'U').charAt(0).toUpperCase() }}
            </span>
            <span class="hidden text-sm font-bold sm:inline">{{ auth.fullName() || 'Account' }}</span>
            <button 
              type="button" 
              class="rounded-lg p-2 text-slate-400 hover:bg-slate-100" 
              (click)="logout()" 
              aria-label="Log out">
              <lucide-icon [img]="ChevronDown" [size]="15" />
            </button>
          </div>
        </div>
      </header>
      <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
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
      </main>
      <app-ai-bot [resumeId]="currentResumeId()" />
    </div>
  `
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly resumeService = inject(ResumeService);
  readonly FileSearch = FileSearch;
  readonly HelpCircle = HelpCircle;
  readonly Bell = Bell;
  readonly ChevronDown = ChevronDown;
  
  readonly progress = signal(0);
  readonly analysis = signal<ResumeAnalysisResponse | null>(null);
  readonly currentResumeId = signal<number | null>(null);
  currentDate = '';

  ngOnInit(): void {
    this.updateCurrentDate();
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

  logout(): void {
    this.auth.logout();
    window.location.reload();
  }
}
