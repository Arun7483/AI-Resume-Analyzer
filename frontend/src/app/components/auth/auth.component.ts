import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, FileSearch, LoaderCircle, LockKeyhole, Mail, UserRound } from 'lucide-angular';
import { AuthService } from '../../services/auth.service';
export class AuthComponent {

  readonly auth = inject(AuthService);

  readonly registerMode = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  fullName = '';
  email = '';
  password = '';

  readonly FileSearch = FileSearch;
  readonly LoaderCircle = LoaderCircle;
  readonly LockKeyhole = LockKeyhole;
  readonly Mail = Mail;
  readonly UserRound = UserRound;

  submit(): void {

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    const request = this.registerMode()
      ? this.auth.register(
          this.fullName,
          this.email,
          this.password
        )
      : this.auth.login(
          this.email,
          this.password
        );

    request.subscribe({

      next: response => {

        if (this.registerMode()) {

          if (response.emailVerified) {
            this.success.set(
              response.message ||
              'Account created successfully.'
            );
          } else {
            this.success.set(
              response.message ||
              'Account created. Please check your email and verify your account.'
            );
          }

        } else {

          this.success.set(
            'Login successful.'
          );
        }
      },

      error: response => {

        this.error.set(
          this.getErrorMessage(response)
        );

        this.loading.set(false);
      },

      complete: () => {
        this.loading.set(false);
      }
    });
  }

  private getErrorMessage(
    response: {
      error?: {
        message?: string;
        validationErrors?: Record<string, string>;
      } | string;
      status?: number;
    }
  ): string {

    if (
      typeof response.error === 'string' &&
      response.error.trim()
    ) {
      return response.error;
    }

    if (
      typeof response.error === 'object' &&
      response.error?.message
    ) {
      return response.error.message;
    }

    const validationErrors =
      typeof response.error === 'object'
        ? response.error?.validationErrors
        : undefined;

    if (validationErrors) {
      return Object.values(validationErrors).join(' ');
    }

    if (response.status === 0) {
      return 'Cannot reach the backend. Please try again.';
    }

    if (response.status === 401) {
      return 'Invalid email or password.';
    }

    return 'Authentication failed. Please check your details.';
  }
}