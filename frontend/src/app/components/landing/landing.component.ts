import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Zap, FileCheck2, Bot, Shield, BarChart3, ArrowRight } from 'lucide-angular';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="min-h-screen bg-gradient-to-b from-slate-50 via-white to-slate-50">
      <!-- Navigation -->
      <nav class="sticky top-0 z-50 border-b bg-white/80 backdrop-blur-xl">
        <div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
          <div class="flex items-center gap-2.5 font-bold">
            <span class="grid size-9 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-mint text-white">
              <lucide-icon [img]="FileCheck2" [size]="20" />
            </span>
            <span>Resume<span class="text-brand-600">Pulse</span></span>
          </div>
          <div class="flex items-center gap-3">
            <button 
              (click)="scrollToSection('features')"
              class="hidden px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 sm:inline">
              Features
            </button>
            <button 
              (click)="scrollToSection('pricing')"
              class="hidden px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 sm:inline">
              Pricing
            </button>
            <button 
              (click)="navigateTo('/auth?mode=login')"
              class="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100">
              Sign in
            </button>
            <button 
              (click)="navigateTo('/auth?mode=register')"
              class="rounded-lg bg-gradient-to-r from-brand-500 to-mint px-4 py-2 text-sm font-semibold text-white shadow-lg hover:shadow-xl transition-shadow">
              Get started
            </button>
          </div>
        </div>
      </nav>

      <!-- Hero Section -->
      <section class="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-32">
        <div class="absolute inset-0 -z-10 overflow-hidden">
          <div class="absolute -right-20 -top-20 size-80 rounded-full bg-brand-100/50 blur-3xl"></div>
          <div class="absolute -left-20 bottom-0 size-80 rounded-full bg-mint/20 blur-3xl"></div>
        </div>
        
        <div class="mx-auto max-w-2xl text-center">
          <p class="inline-block rounded-full bg-brand-50 px-4 py-1.5 text-xs font-bold uppercase tracking-widest text-brand-600">
            AI-Powered Resume Analysis
          </p>
          <h1 class="mt-6 text-5xl font-black tracking-tight sm:text-6xl bg-gradient-to-r from-brand-600 to-mint bg-clip-text text-transparent">
            Get Your Resume Past Every Screening System
          </h1>
          <p class="mt-6 text-lg leading-8 text-slate-600">
            Harness the power of AI to receive intelligent, actionable feedback on your resume. Optimize for ATS, highlight your best achievements, and land your dream job.
          </p>
          <div class="mt-10 flex items-center justify-center gap-4">
            <button 
              (click)="navigateTo('/auth?mode=register')"
              class="rounded-lg bg-gradient-to-r from-brand-500 to-mint px-6 py-3 text-base font-semibold text-white shadow-xl hover:shadow-2xl transition-all hover:scale-105">
              Start Free Analysis
            </button>
            <button 
              (click)="scrollToSection('features')"
              class="flex items-center gap-2 rounded-lg px-6 py-3 text-base font-semibold text-slate-700 hover:bg-slate-100">
              Learn More
              <lucide-icon [img]="ArrowRight" [size]="18" />
            </button>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section #features class="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div class="text-center">
          <h2 class="text-3xl font-bold tracking-tight sm:text-4xl">Powerful Features</h2>
          <p class="mt-4 max-w-2xl mx-auto text-lg text-slate-600">Everything you need to create a standout resume</p>
        </div>

        <div class="mt-16 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
          <!-- Feature 1 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="Zap" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">AI-Powered Analysis</h3>
            <p class="mt-2 text-slate-600">
              Get instant, intelligent feedback powered by advanced AI that understands what recruiters and ATS systems are looking for.
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="BarChart3" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">ATS Score & Keywords</h3>
            <p class="mt-2 text-slate-600">
              See exactly how your resume scores against ATS systems and get specific keyword recommendations to improve your match rate.
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="Bot" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">Interactive AI Chat</h3>
            <p class="mt-2 text-slate-600">
              Ask follow-up questions and dive deeper into the analysis. Our AI is ready to help you optimize every section.
            </p>
          </div>

          <!-- Feature 4 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="FileCheck2" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">Strengths & Improvements</h3>
            <p class="mt-2 text-slate-600">
              Understand what makes your resume stand out and get concrete suggestions on exactly what to improve.
            </p>
          </div>

          <!-- Feature 5 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="Shield" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">Privacy First</h3>
            <p class="mt-2 text-slate-600">
              Your resume stays private and secure. We encrypt everything and automatically delete your files after 30 days.
            </p>
          </div>

          <!-- Feature 6 -->
          <div class="rounded-2xl border border-slate-200 bg-white p-8 hover:shadow-xl transition-shadow">
            <div class="inline-flex rounded-xl bg-brand-50 p-3">
              <lucide-icon [img]="ArrowRight" class="text-brand-600" [size]="24" />
            </div>
            <h3 class="mt-4 text-lg font-bold">Actionable Insights</h3>
            <p class="mt-2 text-slate-600">
              Get specific, prioritized recommendations that you can implement immediately to improve your resume.
            </p>
          </div>
        </div>
      </section>

      <!-- How It Works Section -->
      <section class="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div class="text-center">
          <h2 class="text-3xl font-bold tracking-tight sm:text-4xl">How It Works</h2>
          <p class="mt-4 max-w-2xl mx-auto text-lg text-slate-600">Get AI insights about your resume in three simple steps</p>
        </div>

        <div class="mt-16 grid gap-8 sm:grid-cols-3">
          <div class="text-center">
            <div class="mx-auto flex size-16 items-center justify-center rounded-full bg-brand-100">
              <span class="text-2xl font-bold text-brand-600">1</span>
            </div>
            <h3 class="mt-4 text-lg font-bold">Upload Your Resume</h3>
            <p class="mt-2 text-slate-600">Simply upload your PDF or DOCX resume to get started with the analysis.</p>
          </div>

          <div class="text-center">
            <div class="mx-auto flex size-16 items-center justify-center rounded-full bg-brand-100">
              <span class="text-2xl font-bold text-brand-600">2</span>
            </div>
            <h3 class="mt-4 text-lg font-bold">Get AI Feedback</h3>
            <p class="mt-2 text-slate-600">Our AI instantly analyzes your resume and provides detailed insights and recommendations.</p>
          </div>

          <div class="text-center">
            <div class="mx-auto flex size-16 items-center justify-center rounded-full bg-brand-100">
              <span class="text-2xl font-bold text-brand-600">3</span>
            </div>
            <h3 class="mt-4 text-lg font-bold">Optimize & Improve</h3>
            <p class="mt-2 text-slate-600">Use the feedback to improve your resume and increase your chances of getting hired.</p>
          </div>
        </div>
      </section>

      <!-- CTA Section -->
      <section class="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div class="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-600 to-mint px-8 py-16 shadow-2xl sm:px-12 sm:py-20">
          <div class="absolute inset-0 -z-10 opacity-10">
            <div class="absolute -right-20 -top-20 size-80 rounded-full bg-white blur-3xl"></div>
          </div>
          <div class="mx-auto max-w-2xl text-center">
            <h2 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
              Ready to Stand Out?
            </h2>
            <p class="mt-4 text-lg text-white/90">
              Start getting AI-powered feedback on your resume today. It takes less than a minute to upload and get insights.
            </p>
            <button 
              (click)="navigateTo('/auth?mode=register')"
              class="mt-8 inline-flex items-center rounded-lg bg-white px-6 py-3 text-base font-semibold text-brand-600 shadow-xl hover:shadow-2xl transition-all hover:scale-105">
              Get Started Free
              <lucide-icon [img]="ArrowRight" class="ml-2" [size]="18" />
            </button>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="border-t border-slate-200 bg-slate-50/50">
        <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6">
          <div class="flex flex-col items-center justify-between gap-4 sm:flex-row">
            <div class="flex items-center gap-2 font-bold">
              <span class="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-brand-500 to-mint text-white">
                <lucide-icon [img]="FileCheck2" [size]="18" />
              </span>
              Resume<span class="text-brand-600">Pulse</span>
            </div>
            <div class="flex gap-6 text-sm text-slate-600">
              <button class="hover:text-slate-900">Privacy Policy</button>
              <button class="hover:text-slate-900">Terms of Service</button>
              <button class="hover:text-slate-900">Contact</button>
            </div>
            <p class="text-sm text-slate-500">© 2026 ResumePulse. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  `
})
export class LandingComponent {
  private readonly router = inject(Router);
  readonly FileCheck2 = FileCheck2;
  readonly Zap = Zap;
  readonly Bot = Bot;
  readonly Shield = Shield;
  readonly BarChart3 = BarChart3;
  readonly ArrowRight = ArrowRight;

  navigateTo(path: string): void {
    void this.router.navigateByUrl(path);
  }

  scrollToSection(section: string): void {
    const element = document.getElementById(section) || document.querySelector(`#${section}`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
