import { Component, inject, signal } from '@angular/core';
import { FormField, FormRoot, form, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, errorCode } from './auth.service';
import { ResendVerification } from './resend-verification';

@Component({
  selector: 'app-login',
  imports: [FormField, FormRoot, ResendVerification, RouterLink],
  template: `
    <h1 class="mb-4 text-3xl font-bold">Autentificare</h1>
    <form [formRoot]="loginForm">
      <label class="form-label" for="email">Email</label>
      <input
        class="form-input"
        id="email"
        type="email"
        autocomplete="email"
        [formField]="loginForm.email"
      />

      <label class="form-label" for="password">Parolă</label>
      <input
        class="form-input"
        id="password"
        type="password"
        autocomplete="current-password"
        [formField]="loginForm.password"
      />

      <button class="btn-primary" type="submit" [disabled]="loginForm().submitting()">
        Intră în cont
      </button>
    </form>

    @if (loginForm().touched() && loginForm().invalid()) {
      <p role="alert" class="error-text">Completează emailul și parola.</p>
    }
    @if (error(); as message) {
      <p role="alert" class="error-text">{{ message }}</p>
    }
    @if (notVerified()) {
      <app-resend-verification [email]="model().email" />
    }
    <p>Nu ai cont? <a class="link" routerLink="/register">Creează unul</a></p>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);
  protected readonly notVerified = signal(false);
  protected readonly model = signal({ email: '', password: '' });
  protected readonly loginForm = form(
    this.model,
    (path) => {
      required(path.email);
      required(path.password);
    },
    { submission: { action: () => this.logIn() } },
  );

  private async logIn(): Promise<void> {
    this.error.set(null);
    this.notVerified.set(false);
    try {
      const { email, password } = this.model();
      await firstValueFrom(this.auth.login(email, password));
      await this.router.navigateByUrl('/');
    } catch (error) {
      switch (errorCode(error)) {
        case 'EMAIL_NOT_VERIFIED':
          this.error.set('Confirmă-ți adresa de email folosind linkul primit.');
          this.notVerified.set(true);
          break;
        case 'INVALID_CREDENTIALS':
          this.error.set('Email sau parolă greșită.');
          break;
        default:
          this.error.set('A apărut o problemă. Încearcă din nou.');
      }
    }
  }
}
