import { Component, computed, inject } from '@angular/core';
import { Bell, ChevronDown, FileSearch, HelpCircle, LucideAngularModule } from 'lucide-angular';
import { AnalysisDashboardComponent } from './components/analysis-dashboard/analysis-dashboard.component';
import { AiBotComponent } from './components/ai-bot/ai-bot.component';
import { ResumeUploadComponent } from './components/resume-upload/resume-upload.component';
import { ResumeService } from './services/resume.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LucideAngularModule, ResumeUploadComponent, AnalysisDashboardComponent, AiBotComponent],
  template: `
<div class="min-h-screen bg-slate-50 text-slate-900">
  <header class="sticky top-0 z-30 border-b bg-white/90 backdrop-blur-xl">
    <div class="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6">
      <a class="flex items-center gap-2.5 font-bold">
        <span class="grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-mint text-white">
          <lucide-icon [img]="FileSearch" [size]="20" />
        </span>
        Resume<span class="-ml-2.5 text-brand-600">Pulse</span>
      </a>
      <nav class="ml-10 hidden gap-1 md:flex">
        <a class="rounded-lg bg-brand-50 px-3 py-2 text-sm font-bold text-brand-700">Dashboard</a>
        <a class="px-3 py-2 text-sm text-slate-500">My resumes</a>
        <a class="px-3 py-2 text-sm text-slate-500">Job matches</a>
      </nav>
      <div class="ml-auto flex items-center gap-3">
        <lucide-icon [img]="HelpCircle" class="text-slate-400" />
        <lucide-icon [img]="Bell" class="text-slate-400" />
        <span class="grid h-8 w-8 place-items-center rounded-full bg-brand-500 text-xs font-bold text-white">AM</span>
        <span class="hidden text-sm font-bold sm:inline">Alex Morgan</span>
        <lucide-icon [img]="ChevronDown" [size]="15" />
      </div>
    </div>
  </header>
  <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
    <div class="mb-8 rounded-3xl border border-slate-200 bg-white p-6 shadow-soft">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[.24em] text-brand-600">ResumePulse Dashboard</p>
          <h1 class="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">{{ headline() }}</h1>
          <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-500">AI-powered feedback that helps your experience get noticed and pass every screening system.</p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="rounded-3xl bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[.18em] text-slate-400">Status</p>
            <p class="mt-2 text-sm font-bold text-slate-700">{{ statusLabel() }}</p>
          </div>
          <div class="rounded-3xl bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[.18em] text-slate-400">Resume</p>
            <p class="mt-2 text-sm font-bold text-slate-700">{{ analysis()?.fileName ?? 'No resume uploaded yet' }}</p>
          </div>
        </div>
      </div>
    </div>
    <div class="grid items-start gap-7 xl:grid-cols-[360px_1fr]">
      <aside class="space-y-4 xl:sticky xl:top-24">
        <app-resume-upload [progress]="progress()" (upload)="upload($event)" />
        <div class="rounded-3xl bg-brand-50 p-4">
          <p class="text-xs font-bold text-brand-700">Your files stay private</p>
          <p class="mt-1 text-xs text-slate-500">Encrypted in transit and automatically deleted after 30 days.</p>
        </div>
      </aside>
      <section class="space-y-6">
        @if(analysis(); as data){
          <app-analysis-dashboard [analysis]="data" />
          @if(data.id) {
            <app-ai-bot [resumeId]="data.id" />
          }
        } @else {
          <div class="rounded-3xl bg-white p-8 text-center text-slate-500 shadow-soft">
            Upload a resume to see your score, ATS match, strengths, and improvement recommendations.
          </div>
        }
      </section>
    </div>
  </main>
</div>`
})
export class AppComponent {
  readonly FileSearch = FileSearch;
  readonly HelpCircle = HelpCircle;
  readonly Bell = Bell;
  readonly ChevronDown = ChevronDown;
  readonly service = inject(ResumeService);
  readonly analysis = this.service.analysis;
  readonly progress = this.service.uploadProgress;
  readonly status = this.service.status;
  readonly headline = computed(() => this.analysis()?.candidateName ? `Here's how ${this.analysis()?.candidateName} stands out` : 'Make your next application count.');
  readonly statusLabel = computed(() => {
    switch (this.status()) {
      case 'uploading': return 'Uploading resume';
      case 'analyzing': return 'Analyzing results';
      case 'complete': return 'Analysis complete';
      case 'error': return 'Upload error';
      default: return 'Ready to analyze';
    }
  });
  upload(file: File) { this.service.uploadAndAnalyze(file); }
}
