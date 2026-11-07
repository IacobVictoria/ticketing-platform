import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

function withToken<T>(request: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

/**
 * Adds the access token to requests. When it has expired (401), refreshes it once and repeats the request.
 * A 401 from /api/auth/* means wrong password or expired session, so refreshing would not help there.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);

  return next(withToken(request, auth.accessToken())).pipe(
    catchError((error: unknown) => {
      const canRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !request.url.startsWith('/api/auth/');
      if (!canRefresh) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(() => throwError(() => error)),
        switchMap((response) => next(withToken(request, response.accessToken))),
      );
    }),
  );
};
