import { Component, inject, input, signal } from '@angular/core';
import { AuthService } from './auth.service';

/** "Didn't get the email?" button. The backend answers the same whether an email was sent or not. */
@Component({
  selector: 'app-resend-verification',
  template: `
    <button type="button" class="btn-secondary" [disabled]="sending()" (click)="resend()">
      Retrimite emailul de confirmare
    </button>
    <p role="status">
      @if (sent()) {
        Dacă adresa are un cont neconfirmat, vei primi un email în cel mult un minut. Verifică și
        folderul de spam.
      }
    </p>
    <p role="alert">
      @if (failed()) {
        Nu am putut trimite cererea. Încearcă din nou.
      }
    </p>
  `,
})
export class ResendVerification {
  private readonly auth = inject(AuthService);

  readonly email = input.required<string>();

  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly failed = signal(false);

  protected resend(): void {
    this.sending.set(true);
    this.sent.set(false);
    this.failed.set(false);
    this.auth.resendVerification(this.email()).subscribe({
      next: () => {
        this.sent.set(true);
        this.sending.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.sending.set(false);
      },
    });
  }
}
