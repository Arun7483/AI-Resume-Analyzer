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


    this.token.set(
      response.accessToken
    );


    this.fullName.set(
      response.fullName ?? ''
    );
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