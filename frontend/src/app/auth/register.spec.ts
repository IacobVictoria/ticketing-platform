import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let backend: HttpTestingController;

  function type(id: string, value: string): void {
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(`#${id}`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
  });

  it('shows the same "check your email" message whether or not the email already had an account', async () => {
    type('firstName', 'Ana');
    type('lastName', 'Pop');
    type('email', 'ana@example.ro');
    type('password', 'parola-sigura');
    type('confirmPassword', 'parola-sigura');
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    backend.expectOne('/api/auth/register').flush(null, { status: 202, statusText: 'Accepted' });
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();

    expect(element.textContent).toContain('Ți-am trimis un email la ana@example.ro');
  });
});
