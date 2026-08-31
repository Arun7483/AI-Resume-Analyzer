import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  LucideAngularModule,
  FileSearch,
  LoaderCircle,
  LockKeyhole,
  Mail,
  UserRound
} from 'lucide-angular';

import {
  AuthService
} from '../../services/auth.service';


@Component({

  selector: 'app-auth',

  standalone: true,

  imports: [
    FormsModule,
    LucideAngularModule
  ],

  template: `

    <main
      class="grid min-h-screen place-items-center bg-slate-950 px-4 py-10"
    >

      <section
        class="w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl sm:p-9"
      >

        <!-- LOGO -->

        <div class="mb-8 text-center">

          <span
            class="mx-auto grid size-12 place-items-center rounded-2xl bg-gradient-to-br from-brand-500 to-mint text-white"
          >

            <lucide-icon
              [img]="FileSearch"
              [size]="24"
            />

          </span>


          <h1
            class="mt-5 text-2xl font-bold text-slate-950"
          >

            Resume
            <span class="text-brand-600">
              Pulse
            </span>

          </h1>


          <p
            class="mt-2 text-sm text-slate-500"
          >

            Sign in to analyze your resume
            with your private AI coach.

          </p>

        </div>


        <!-- LOGIN / REGISTER TABS -->

        <div
          class="mb-6 grid grid-cols-2 rounded-xl bg-slate-100 p-1"
        >

          <button
            type="button"
            class="rounded-lg px-3 py-2 text-sm font-bold"
            [class.bg-white]="!registerMode()"
            (click)="switchMode(false)"
          >

            Sign in

          </button>


          <button
            type="button"
            class="rounded-lg px-3 py-2 text-sm font-bold"
            [class.bg-white]="registerMode()"
            (click)="switchMode(true)"
          >

            Create account

          </button>

        </div>


        <!-- FORM -->

        <form
          class="space-y-4"
          (ngSubmit)="submit()"
        >


          <!-- FULL NAME -->

          @if (registerMode()) {

            <label class="block">

              <span
                class="mb-1 block text-xs font-bold text-slate-600"
              >

                Full name

              </span>


              <span
                class="flex items-center gap-2 rounded-xl border px-3"
              >

                <lucide-icon
                  [img]="UserRound"
                  [size]="17"
                  class="text-slate-400"
                />


                <input
                  name="fullName"
                  [(ngModel)]="fullName"
                  required
                  class="w-full py-3 text-sm outline-none"
                  autocomplete="name"
                  placeholder="Your full name"
                />

              </span>

            </label>

          }


          <!-- EMAIL -->

          <label class="block">

            <span
              class="mb-1 block text-xs font-bold text-slate-600"
            >

              Email

            </span>


            <span
              class="flex items-center gap-2 rounded-xl border px-3"
            >

              <lucide-icon
                [img]="Mail"
                [size]="17"
                class="text-slate-400"
              />


              <input
                name="email"
                [(ngModel)]="email"
                required
                type="email"
                class="w-full py-3 text-sm outline-none"
                autocomplete="email"
                placeholder="you@example.com"
              />

            </span>

          </label>


          <!-- PASSWORD -->

          <label class="block">

            <span
              class="mb-1 block text-xs font-bold text-slate-600"
            >

              Password

            </span>


            <span
              class="flex items-center gap-2 rounded-xl border px-3"
            >

              <lucide-icon
                [img]="LockKeyhole"
                [size]="17"
                class="text-slate-400"
              />


              <input
                name="password"
                [(ngModel)]="password"
                required
                minlength="8"
                type="password"
                class="w-full py-3 text-sm outline-none"
                autocomplete="current-password"
                placeholder="Minimum 8 characters"
              />

            </span>

          </label>


          <!-- MESSAGE -->

          @if (message()) {

            <p
              class="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
              role="status"
            >

              {{ message() }}

            </p>

          }


          <!-- ERROR -->

          @if (error()) {

            <p
              class="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700"
              role="alert"
            >

              {{ error() }}

            </p>

          }


          <!-- SUBMIT -->

          <button
            type="submit"
            class="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 py-3 font-bold text-white disabled:opacity-60"
            [disabled]="loading()"
          >

            <lucide-icon
              [img]="LoaderCircle"
              [size]="17"
              [class.animate-spin]="loading()"
            />


            {{
              loading()
                ? 'Please wait...'
                : registerMode()
                  ? 'Create account'
                  : 'Sign in'
            }}

          </button>

        </form>


        <p
          class="mt-6 text-center text-xs text-slate-400"
        >

          Your account is protected with
          JWT authentication.

        </p>

      </section>

    </main>

  `
})
export class AuthComponent {

  readonly auth =
    inject(AuthService);


  readonly registerMode =
    signal(false);


  readonly loading =
    signal(false);


  readonly error =
    signal('');


  readonly message =
    signal('');


  fullName = '';

  email = '';

  password = '';


  readonly FileSearch =
    FileSearch;

  readonly LoaderCircle =
    LoaderCircle;

  readonly LockKeyhole =
    LockKeyhole;

  readonly Mail =
    Mail;

  readonly UserRound =
    UserRound;


  switchMode(
    register: boolean
  ): void {

    this.registerMode.set(
      register
    );

    this.error.set('');

    this.message.set('');
  }


  submit(): void {

    this.loading.set(true);

    this.error.set('');

    this.message.set('');


    const request =
      this.registerMode()

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

        /*
         * Registration requiring
         * email verification.
         */
        if (
          this.registerMode()
        ) {

          if (
            response.emailVerified
          ) {

            this.message.set(
              response.message ||
              'Account created successfully.'
            );

          } else {

            this.message.set(
              response.message ||
              'Account created. Please check your email and verify your account.'
            );

          }

          return;
        }


        /*
         * Login successful.
         */
        this.message.set(
          response.message ||
          'Login successful.'
        );

      },


      error: response => {

        console.error(
          'Authentication error:',
          response
        );


        this.error.set(
          this.getErrorMessage(
            response
          )
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
      error?:
        | {
            message?: string;
            validationErrors?:
              Record<string, string>;
          }
        | string;

      status?: number;
    }
  ): string {

    /*
     * Plain backend response
     */
    if (
      typeof response.error === 'string' &&
      response.error.trim()
    ) {

      return response.error;
    }


    /*
     * JSON message
     */
    if (
      typeof response.error === 'object' &&
      response.error?.message
    ) {

      return response.error.message;
    }


    /*
     * Validation errors
     */
    const validationErrors =
      typeof response.error === 'object'
        ? response.error?.validationErrors
        : undefined;


    if (
      validationErrors
    ) {

      return Object.values(
        validationErrors
      ).join(' ');
    }


    /*
     * Backend unreachable / CORS / network.
     */
    if (
      response.status === 0
    ) {

      return (
        'Cannot reach the backend. ' +
        'Please check the Render backend and CORS configuration.'
      );
    }


    /*
     * Unauthorized
     */
    if (
      response.status === 401
    ) {

      return (
        'Invalid email/password, or your email has not been verified.'
      );
    }


    return (
      'Authentication failed. Please check your details.'
    );
  }

}