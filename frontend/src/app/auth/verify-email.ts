import { Component, inject, input, signal } from '@angular/core';
import { FormField, FormRoot, form, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, errorCode } from './auth.service';
import { ResendVerification } from './resend-verification';

type Status = 'waiting' | 'verified' | 'invalidLink';

/** The page the link from the email opens: /verify-email?token=... */
@Component({
  selector: 'app-verify-email',
  imports: [FormField, FormRoot, ResendVerification, RouterLink],
  template: `
    <h1 class="mb-4 text-3xl font-bold">Confirmă adresa de email</h1>
    @switch (status()) {
      @case ('verified') {
        <p role="status">Contul tău este activ.</p>
        <p><a class="link" routerLink="/login">Intră în cont</a></p>
      }
      @case ('invalidLink') {
        <p role="alert" class="error-text">
          Linkul nu mai este valabil. Poate ai confirmat deja contul: încearcă să
          <a class="link" routerLink="/login">te autentifici</a>.
        </p>
        <p>Altfel, scrie adresa de email și îți trimitem un link nou.</p>
        <label class="form-label" for="resendEmail">Email</label>
        <input
          class="form-input"
          id="resendEmail"
          type="email"
          autocomplete="email"
          [formField]="verifyForm.email"
        />
        @if (model().email) {
          <app-resend-verification [email]="model().email" />
        }
      }
      @default {
        @if (token()) {
          <p>Introdu parola aleasă la înregistrare, ca să dovedești că tu ai creat contul.</p>
          <form [formRoot]="verifyForm">
            <label class="form-label" for="password">Parolă</label>
            <input
              class="form-input"
              id="password"
              type="password"
              autocomplete="current-password"
              [formField]="verifyForm.password"
            />
            <button class="btn-primary" type="submit" [disabled]="verifyForm().submitting()">
              Confirmă contul
            </button>
          </form>
          @if (verifyForm().touched() && verifyForm().invalid()) {
            <p role="alert" class="error-text">Introdu parola.</p>
          }
          @if (error(); as message) {
            <p role="alert" class="error-text">{{ message }}</p>
          }
        } @else {
          <p role="alert" class="error-text">
            Linkul este incomplet. Deschide-l din nou din email.
          </p>
        }
      }
    }
  `,
})
export class VerifyEmail {
  private readonly auth = inject(AuthService);

  /** Filled by the router from ?token=... */
  readonly token = input<string>();

  protected readonly status = signal<Status>('waiting');
  protected readonly error = signal<string | null>(null);
  protected readonly model = signal({ password: '', email: '' });
  protected readonly verifyForm = form(
    this.model,
    (path) => {
      required(path.password);
    },
    { submission: { action: () => this.verify() } },
  );

  private async verify(): Promise<void> {
    this.error.set(null);
    try {
      await firstValueFrom(this.auth.verifyEmail(this.token() ?? '', this.model().password));
      this.status.set('verified');
    } catch (error) {
      switch (errorCode(error)) {
        case 'INVALID_VERIFICATION_TOKEN':
          this.status.set('invalidLink');
          break;
        case 'INVALID_CREDENTIALS':
          this.error.set(
            'Parola nu este cea a contului. Dacă nu tu ai creat contul, poți ignora emailul sau poți crea un cont nou cu această adresă.',
          );
          break;
        default:
          this.error.set('A apărut o problemă. Încearcă din nou.');
      }
    }
  }
}
