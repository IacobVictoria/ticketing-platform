import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <h1 class="mb-4 text-3xl font-bold">Ticketing</h1>
    @if (auth.user(); as user) {
      <p>Bun venit, {{ user.firstName }}!</p>
      <button type="button" class="btn-secondary" (click)="logout()">Ieși din cont</button>
    } @else {
      <p>
        <a class="link" routerLink="/login">Intră în cont</a> sau
        <a class="link" routerLink="/register">creează unul</a>.
      </p>
    }
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);

  protected logout(): void {
    this.auth.logout().subscribe({ error: () => undefined });
  }
}
