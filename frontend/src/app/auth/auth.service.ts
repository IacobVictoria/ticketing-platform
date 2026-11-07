import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'USER' | 'ADMIN' | 'STAFF';
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface RegisterData {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
}

/** The code the backend puts in its error responses, e.g. EMAIL_NOT_VERIFIED. */
export function errorCode(error: unknown): string | undefined {
  return error instanceof HttpErrorResponse ? error.error?.code : undefined;
}

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  /** Only in memory; after a page reload it comes back through refresh(), from the HttpOnly cookie. */
  private readonly token = signal<string | null>(null);
  private readonly currentUser = signal<User | null>(null);
  private refreshInProgress: Observable<AuthResponse> | null = null;

  readonly accessToken = this.token.asReadonly();
  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  /** Always 202 with no body: the answer does not say whether the email already had an account. */
  register(data: RegisterData): Observable<void> {
    return this.http.post<void>('/api/auth/register', data);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.logIn(response)));
  }

  /** Everyone asking while a refresh is running gets the same one: the token can be used only once. */
  refresh(): Observable<AuthResponse> {
    if (!this.refreshInProgress) {
      this.refreshInProgress = this.http.post<AuthResponse>('/api/auth/refresh', null).pipe(
        tap((response) => this.logIn(response)),
        catchError((error) => {
          this.clear();
          return throwError(() => error);
        }),
        finalize(() => (this.refreshInProgress = null)),
        shareReplay(1),
      );
    }
    return this.refreshInProgress;
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null).pipe(finalize(() => this.clear()));
  }

  verifyEmail(token: string, password: string): Observable<void> {
    return this.http.post<void>('/api/auth/verify-email', { token, password });
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/resend-verification', { email });
  }

  private logIn(response: AuthResponse): void {
    this.token.set(response.accessToken);
    this.currentUser.set(response.user);
  }

  private clear(): void {
    this.token.set(null);
    this.currentUser.set(null);
  }
}
