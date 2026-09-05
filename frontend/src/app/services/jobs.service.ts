import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { API_BASE_URL } from '../api.config';
import { JobMatch } from '../models/job.model';
import { ResumeService } from './resume.service';

@Injectable({ providedIn: 'root' })
export class JobsService {
  private readonly http = inject(HttpClient);
  private readonly resumes = inject(ResumeService);
  private readonly endpoint = `${API_BASE_URL}/api/v1/resumes/job-matches`;

  readonly jobs = signal<JobMatch[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  loadMatches(): void {
    this.loading.set(true);
    this.error.set('');
    this.http.get<JobMatch[]>(this.endpoint).subscribe({
      next: jobs => {
        this.jobs.set(jobs.length ? jobs : this.buildFallbackJobs());
        this.loading.set(false);
      },
      error: response => {
        this.jobs.set(this.buildFallbackJobs());
        this.error.set('Live listings are temporarily unavailable. Showing active search links instead.');
        this.loading.set(false);
      }
    });
  }

  private buildFallbackJobs(): JobMatch[] {
    const analysis = this.resumes.analysis();
    const resumeText = [
      analysis?.fileName,
      analysis?.candidateName,
      ...(analysis?.strengths ?? []).map(item => item.description),
      ...(analysis?.improvements ?? []).map(item => item.description),
      ...(analysis?.keywords ?? []).map(item => item.keyword)
    ].join(' ').toLowerCase();

    const roles = resumeText.match(/hardware|electronics|embedded|microcontroller|pcb|fpga|verilog|vlsi|circuit|firmware|arduino|raspberry|instrumentation/)
      ? ['Electronics Engineer', 'Embedded Systems Engineer', 'Hardware Design Engineer', 'PCB Design Engineer']
      : resumeText.match(/data|sql|tableau|power bi|python|statistics|analytics/)
        ? ['Data Analyst', 'Business Analyst', 'Data Scientist', 'BI Analyst']
        : resumeText.match(/design|figma|ux|ui|user research|prototype/)
          ? ['UX Designer', 'UI Designer', 'Product Designer', 'UX Researcher']
          : resumeText.match(/product|roadmap|agile|scrum|stakeholder/)
            ? ['Product Manager', 'Product Analyst', 'Business Analyst', 'Program Manager']
            : ['Software Engineer', 'Frontend Developer', 'Backend Developer', 'QA Engineer'];

    return roles.flatMap((role, index) => {
      const query = encodeURIComponent(role);
      const slug = role.toLowerCase().replace(/\s+/g, '-');
      const links = [
        ['LinkedIn', `https://www.linkedin.com/jobs/search/?keywords=${query}`],
        ['Naukri', `https://www.naukri.com/${slug}-jobs`],
        ['Foundit', `https://www.foundit.in/srp/results?query=${query}`],
        ['Internshala', `https://internshala.com/jobs/keywords-${slug}/`]
      ];
      return links.map(([portal, url]) => ({
        title: role,
        company: `${portal} active job search`,
        location: 'Current listings',
        description: `Browse current ${role} listings on ${portal} and apply manually on the original platform.`,
        applyUrl: url,
        matchPercentage: Math.max(50, 72 - index * 5),
        remote: false
      }));
    });
  }
}
