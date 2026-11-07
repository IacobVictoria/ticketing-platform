import { Component, inject, signal } from '@angular/core';
import {
  FormField,
  FormRoot,
  email,
  form,
  minLength,
  required,
  validate,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, errorCode } from './auth.service';
import { ResendVerification } from './resend-verification';

@Component({
  selector: 'app-register',
  imports: [FormField, FormRoot, ResendVerification, RouterLink],
  template: `
    <h1 class="mb-4 text-3xl font-bold">Creează cont</h1>
    @if (registeredEmail(); as address) {
      <p role="status">
        Ți-am trimis un email la <strong>{{ address }}</strong
        >. Urmează pașii din el ca să-ți activezi contul. Dacă ai deja un cont cu această adresă,
        emailul îți spune cum să intri în el.
      </p>
      <app-resend-verification [email]="address" />
    } @else {
      <form [formRoot]="registerForm">
        <label class="form-label" for="firstName">Prenume</label>
        <input
          class="form-input"
          id="firstName"
          autocomplete="given-name"
          [formField]="registerForm.firstName"
        />

        <label class="form-label" for="lastName">Nume</label>
        <input
          class="form-input"
          id="lastName"
          autocomplete="family-name"
          [formField]="registerForm.lastName"
        />

        <label class="form-label" for="email">Email</label>
        <input
          class="form-input"
          id="email"
          type="email"
          autocomplete="email"
          [formField]="registerForm.email"
        />

        <label class="form-label" for="password">Parolă (minimum 8 caractere)</label>
        <input
          class="form-input"
          id="password"
          type="password"
          autocomplete="new-password"
          [formField]="registerForm.password"
        />

        <label class="form-label" for="confirmPassword">Confirmă parola</label>
        <input
          class="form-input"
          id="confirmPassword"
          type="password"
          autocomplete="new-password"
          [formField]="registerForm.confirmPassword"
        />

        <button class="btn-primary" type="submit" [disabled]="registerForm().submitting()">
          Creează contul
        </button>
      </form>

      @if (registerForm().touched() && registerForm().invalid()) {
        <p role="alert" class="error-text">
          Completează toate câmpurile, cu un email valid și o parolă de minimum 8 caractere,
          identică în ambele câmpuri.
        </p>
      }
      @if (error(); as message) {
        <p role="alert" class="error-text">{{ message }}</p>
      }
      <p>Ai deja cont? <a class="link" routerLink="/login">Intră în cont</a></p>
    }
  `,
})
export class Register {
  private readonly auth = inject(AuthService);

  protected readonly error = signal<string | null>(null);
  protected readonly registeredEmail = signal<string | null>(null);
  protected readonly model = signal({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  protected readonly registerForm = form(
    this.model,
    (path) => {
      required(path.firstName);
      required(path.lastName);
      required(path.email);
      email(path.email);
      required(path.password);
      minLength(path.password, 8);
      required(path.confirmPassword);
      validate(path.confirmPassword, ({ value, valueOf }) =>
        value() === valueOf(path.password) ? null : { kind: 'passwordsDiffer' },
      );
    },
    { submission: { action: () => this.register() } },
  );

  private async register(): Promise<void> {
    this.error.set(null);
    try {
      await firstValueFrom(this.auth.register(this.model()));
      this.registeredEmail.set(this.model().email.trim());
    } catch (error) {
      switch (errorCode(error)) {
        case 'PASSWORD_TOO_LONG':
          this.error.set('Parola este prea lungă.');
          break;
        case 'VALIDATION_FAILED':
          this.error.set('Verifică datele introduse.');
          break;
        default:
          this.error.set('A apărut o problemă. Încearcă din nou.');
      }
    }
  }
}
