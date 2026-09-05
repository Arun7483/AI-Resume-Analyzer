import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BriefcaseBusiness, ExternalLink, LoaderCircle, MapPin, RefreshCw, Sparkles, LucideAngularModule } from 'lucide-angular';
import { JobsService } from '../../services/jobs.service';

@Component({
  selector: 'app-job-matches',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section aria-labelledby="jobs-title">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-xs font-bold uppercase tracking-[.18em] text-brand-600">Live opportunities</p>
          <h2 id="jobs-title" class="mt-1 text-3xl font-bold text-slate-950">Jobs matched to your resume</h2>
          <p class="mt-2 max-w-2xl text-sm text-slate-500">Fresh listings ranked by the skills and experience in your latest uploaded resume.</p>
        </div>
        <button type="button" class="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-700 shadow-sm" (click)="jobs.loadMatches()" [disabled]="jobs.loading()">
          <lucide-icon [img]="RefreshCw" [size]="15" [class.animate-spin]="jobs.loading()" /> Refresh
        </button>
      </div>

      @if (jobs.loading()) {
        <div class="mt-8 grid place-items-center rounded-2xl border border-slate-200 bg-white py-16 text-sm text-slate-500">
          <lucide-icon [img]="LoaderCircle" [size]="24" class="mb-3 animate-spin text-brand-500" />
          Finding active roles for your profile...
        </div>
      } @else if (jobs.error() && !jobs.jobs().length) {
        <div class="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">{{ jobs.error() }}</div>
      } @else if (!jobs.jobs().length) {
        <div class="mt-8 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
          <lucide-icon [img]="BriefcaseBusiness" [size]="30" class="mx-auto text-brand-500" />
          <h3 class="mt-3 font-bold text-slate-900">No active listings are available right now</h3>
          <p class="mt-1 text-sm text-slate-500">Refresh in a moment, or upload a resume and try again after analysis finishes.</p>
        </div>
      } @else {
        <div class="mt-8 grid gap-4 lg:grid-cols-2">
          @for (job of jobs.jobs(); track job.applyUrl) {
            <article class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h3 class="font-bold leading-snug text-slate-950">{{ job.title }}</h3>
                  <p class="mt-1 text-sm font-semibold text-brand-700">{{ job.company }}</p>
                </div>
                <span class="shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">{{ job.matchPercentage }}% match</span>
              </div>
              <div class="mt-3 flex flex-wrap gap-3 text-xs font-semibold text-slate-500">
                <span class="flex items-center gap-1"><lucide-icon [img]="MapPin" [size]="14" />{{ job.location || 'Location not listed' }}</span>
                @if (job.remote) { <span class="rounded-full bg-sky-50 px-2 py-1 text-sky-700">Remote</span> }
              </div>
              <p class="mt-4 line-clamp-3 text-sm leading-relaxed text-slate-600">{{ job.description }}</p>
              <a class="mt-5 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-bold text-white" [href]="job.applyUrl" target="_blank" rel="noopener noreferrer">
                Apply now <lucide-icon [img]="ExternalLink" [size]="15" />
              </a>
            </article>
          }
        </div>
        <p class="mt-5 flex items-center gap-2 text-xs text-slate-400"><lucide-icon [img]="Sparkles" [size]="14" />Listings are provided by a public jobs feed. Applications open on the original job site.</p>
      }
    </section>
  `
})
export class JobMatchesComponent {
  readonly jobs = inject(JobsService);
  readonly BriefcaseBusiness = BriefcaseBusiness;
  readonly ExternalLink = ExternalLink;
  readonly LoaderCircle = LoaderCircle;
  readonly MapPin = MapPin;
  readonly RefreshCw = RefreshCw;
  readonly Sparkles = Sparkles;

  ngOnInit(): void {
    this.jobs.loadMatches();
  }
}
