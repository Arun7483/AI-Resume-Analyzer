import {
  HttpClient,
  HttpInterceptorFn
} from '@angular/common/http';

import {
  computed,
  inject,
  Injectable,
  signal
} from '@angular/core';

import { tap } from 'rxjs';

import { API_BASE_URL } from '../api.config';


export interface AuthResponse {

  accessToken: string | null;

  tokenType: string | null;

  expiresIn: number;

  fullName: string;

  email: string;

  role: string;

  emailVerified: boolean;

  message: string;
}


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);


  private readonly tokenKey =
    'resume-pulse-token';

  private readonly nameKey =
    'resume-pulse-name';

  private readonly emailKey =
    'resume-pulse-email';

  private readonly roleKey =
    'resume-pulse-role';


  readonly token =
    signal<string | null>(
      localStorage.getItem(
        this.tokenKey
      )
    );


  readonly fullName =
    signal<string>(
      localStorage.getItem(
        this.nameKey
      ) ?? ''
    );

  readonly email = signal<string>(localStorage.getItem(this.emailKey) ?? '');

  readonly role = signal<string>(localStorage.getItem(this.roleKey) ?? 'ROLE_USER');


  readonly isAuthenticated =
    computed(
      () => !!this.token()
    );


  /*
   * LOGIN
   */
  login(
    email: string,
    password: string
  ) {

    return this.http
      .post<AuthResponse>(
        `${API_BASE_URL}/api/v1/auth/login`,
        {
          email: email.trim(),
          password: password
        }
      )
      .pipe(

        tap(response => {

          if (
            response.accessToken &&
            response.emailVerified
          ) {

            this.store(response);
          }

        })

      );
  }


  /*
   * REGISTER
   */
  register(
    fullName: string,
    email: string,
    password: string
  ) {

    return this.http
      .post<AuthResponse>(
        `${API_BASE_URL}/api/v1/auth/register`,
        {
          fullName: fullName.trim(),
          email: email.trim(),
          password: password
        }
      )
      .pipe(

        tap(response => {

          /*
           * Do not store a JWT when the
           * backend requires email verification.
           */
          if (
            response.accessToken &&
            response.emailVerified
          ) {

            this.store(response);
          }

        })

      );
  }

  googleLogin(credential: string) {
    return this.http.post<AuthResponse>(
      `${API_BASE_URL}/api/v1/auth/google`,
      { credential }
    ).pipe(
      tap(response => {
        if (response.accessToken && response.emailVerified) {
          this.store(response);
        }
      })
    );
  }

  requestPasswordReset(email: string) {
    return this.http.post<string>(
      `${API_BASE_URL}/api/v1/auth/forgot-password`,
      { email: email.trim() }
    );
  }

  resetPassword(token: string, password: string) {
    return this.http.post<string>(
      `${API_BASE_URL}/api/v1/auth/reset-password`,
      { token, password }
    );
  }


  /*
   * LOGOUT
   */
  logout(): void {

    localStorage.removeItem(
      this.tokenKey
    );

    localStorage.removeItem(
      this.nameKey
    );

    localStorage.removeItem(this.emailKey);
    localStorage.removeItem(this.roleKey);


    this.token.set(null);

    this.fullName.set('');
  }


  /*
   * STORE AUTHENTICATION DATA
   */
  private store(
    response: AuthResponse
  ): void {

    if (
      !response.accessToken
    ) {

      return;
    }


    localStorage.setItem(
      this.tokenKey,
      response.accessToken
    );


    localStorage.setItem(
      this.nameKey,
      response.fullName ?? ''
    );

    localStorage.setItem(this.emailKey, response.email ?? '');
    localStorage.setItem(this.roleKey, response.role ?? 'ROLE_USER');


    this.token.set(
      response.accessToken
    );


    this.fullName.set(
      response.fullName ?? ''
    );

    this.email.set(response.email ?? '');
    this.role.set(response.role ?? 'ROLE_USER');
  }

}


/*
 * =====================================================
 * JWT HTTP INTERCEPTOR
 * =====================================================
 *
 * Automatically sends:
 *
 * Authorization: Bearer <JWT>
 *
 * with requests after login.
 */
export const authInterceptor: HttpInterceptorFn =
  (request, next) => {

    const token =
      localStorage.getItem(
        'resume-pulse-token'
      );


    /*
     * No token = public request.
     */
    if (!token) {

      return next(request);
    }


    /*
     * Add JWT Authorization header.
     */
    const authenticatedRequest =
      request.clone({

        setHeaders: {

          Authorization:
            `Bearer ${token}`

        }

      });


    return next(
      authenticatedRequest
    );
  };