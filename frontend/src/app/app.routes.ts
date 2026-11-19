import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Evenimente',
    loadComponent: () => import('./events/event-list').then((m) => m.EventList),
  },
  {
    path: 'events/:id',
    title: 'Eveniment',
    loadComponent: () => import('./events/event-details').then((m) => m.EventDetails),
  },
  {
    path: 'login',
    title: 'Autentificare',
    loadComponent: () => import('./auth/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Creează cont',
    loadComponent: () => import('./auth/register').then((m) => m.Register),
  },
  {
    path: 'verify-email',
    title: 'Confirmă adresa de email',
    loadComponent: () => import('./auth/verify-email').then((m) => m.VerifyEmail),
  },
  { path: '**', redirectTo: '' },
];
