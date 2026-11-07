import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthResponse, AuthService } from './auth.service';

const user = {
  id: '1',
  email: 'ana@example.ro',
  firstName: 'Ana',
  lastName: 'Pop',
  role: 'USER',
  createdAt: '2026-11-07T10:00:00Z',
} as const;

function authResponse(accessToken: string): AuthResponse {
  return { accessToken, tokenType: 'Bearer', expiresIn: 900, user };
}

describe('authentication', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  function logIn(token: string): void {
    auth.login('ana@example.ro', 'parola-sigura').subscribe();
    backend.expectOne('/api/auth/login').flush(authResponse(token));
  }

  it('keeps the user after login and adds the token to requests', () => {
    logIn('token-1');

    expect(auth.isLoggedIn()).toBe(true);
    http.get('/api/orders').subscribe();
    const request = backend.expectOne('/api/orders');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-1');
    request.flush([]);
  });

  it('refreshes only once for simultaneous 401s and repeats both requests with the new token', () => {
    logIn('old-token');
    const results: string[] = [];
    http.get<string>('/api/first').subscribe((value) => results.push(value));
    http.get<string>('/api/second').subscribe((value) => results.push(value));

    backend.expectOne('/api/first').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/second').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush(authResponse('new-token'));

    const first = backend.expectOne('/api/first');
    const second = backend.expectOne('/api/second');
    expect(first.request.headers.get('Authorization')).toBe('Bearer new-token');
    expect(second.request.headers.get('Authorization')).toBe('Bearer new-token');
    first.flush('a');
    second.flush('b');
    expect(results).toEqual(['a', 'b']);
  });

  it('does not refresh on a 401 from /api/auth/*', () => {
    let status = 0;
    auth
      .login('ana@example.ro', 'parola-gresita')
      .subscribe({ error: (error) => (status = error.status) });

    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    backend.expectNone('/api/auth/refresh');
  });

  it('logs out locally and reports the original 401 when the refresh fails', () => {
    logIn('old-token');
    let status = 0;
    http.get('/api/orders').subscribe({ error: (error) => (status = error.status) });

    backend.expectOne('/api/orders').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.accessToken()).toBeNull();
  });
});
