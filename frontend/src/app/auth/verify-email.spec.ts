import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { VerifyEmail } from './verify-email';

describe('VerifyEmail', () => {
  let fixture: ComponentFixture<VerifyEmail>;
  let backend: HttpTestingController;

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  async function submit(password: string): Promise<void> {
    const element = fixture.nativeElement as HTMLElement;
    const input = element.querySelector<HTMLInputElement>('#password')!;
    input.value = password;
    input.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  async function answer(status: number, code?: string): Promise<void> {
    backend.expectOne('/api/auth/verify-email').flush(code ? { code } : null, {
      status,
      statusText: 'Status',
    });
    await formActionFinished();
  }

  /** The form's action continues after an await, so it needs one more turn after the response. */
  async function formActionFinished(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(VerifyEmail);
    fixture.componentRef.setInput('token', 'abc');
    await fixture.whenStable();
  });

  it('sends the token and the password, then shows that the account is active', async () => {
    await submit('parola-sigura');

    const request = backend.expectOne('/api/auth/verify-email');
    expect(request.request.body).toEqual({ token: 'abc', password: 'parola-sigura' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await formActionFinished();

    expect(text()).toContain('Contul tău este activ');
  });

  it('keeps the form and explains a wrong password', async () => {
    await submit('parola-gresita');
    await answer(401, 'INVALID_CREDENTIALS');

    expect(text()).toContain('Parola nu este cea a contului');
    expect((fixture.nativeElement as HTMLElement).querySelector('#password')).not.toBeNull();
  });

  it('offers a new link when this one is no longer valid', async () => {
    await submit('parola-sigura');
    await answer(422, 'INVALID_VERIFICATION_TOKEN');

    expect(text()).toContain('Linkul nu mai este valabil');
    expect((fixture.nativeElement as HTMLElement).querySelector('#resendEmail')).not.toBeNull();
  });

  it('explains an incomplete link', async () => {
    fixture.componentRef.setInput('token', undefined);
    await fixture.whenStable();

    expect(text()).toContain('Linkul este incomplet');
  });
});
