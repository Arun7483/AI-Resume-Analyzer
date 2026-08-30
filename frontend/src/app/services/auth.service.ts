import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
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

  private readonly tokenKey = 'resume-pulse-token';
  private readonly nameKey = 'resume-pulse-name';

  readonly token = signal<string | null>(
    localStorage.getItem(this.tokenKey)
  );

  readonly fullName = signal<string>(
    localStorage.getItem(this.nameKey) ?? ''
  );

  readonly isAuthenticated = computed(
    () => !!this.token()
  );

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(
        `${API_BASE_URL}/api/v1/auth/login`,
        {
          email,
          password
        }
      )
      .pipe(
        tap(response => this.store(response))
      );
  }

  register(
    fullName: string,
    email: string,
    password: string
  ) {
    return this.http
      .post<AuthResponse>(
        `${API_BASE_URL}/api/v1/auth/register`,
        {
          fullName,
          email,
          password
        }
      )
      .pipe(
        tap(response => this.store(response))
      );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.nameKey);

    this.token.set(null);
    this.fullName.set('');
  }

  private store(response: AuthResponse): void {

    if (!response.accessToken) {
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

    this.token.set(response.accessToken);
    this.fullName.set(response.fullName ?? '');
  }
}


/**
 * JWT HTTP interceptor
 *
 * Automatically adds:
 *
 * Authorization: Bearer <JWT>
 *
 * to API requests when the user is logged in.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {

  const token = localStorage.getItem('resume-pulse-token');

  if (!token) {
    return next(request);
  }

  const authenticatedRequest = request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authenticatedRequest);
};