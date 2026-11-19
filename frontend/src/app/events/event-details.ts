import { DatePipe } from '@angular/common';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventSummary, VenueDetails } from './event.model';

/** /events/:id — the event, its venue and the venue's sectors. */
@Component({
  selector: 'app-event-details',
  imports: [DatePipe, RouterLink],
  template: `
    <p><a class="link" routerLink="/">← Toate evenimentele</a></p>

    @if (event.isLoading()) {
      <p role="status">Se încarcă evenimentul...</p>
    } @else if (notFound()) {
      <h1 class="mt-4 mb-4 text-3xl font-bold">Evenimentul nu există</h1>
      <p>Poate a fost șters sau linkul este greșit.</p>
    } @else if (event.error()) {
      <p role="alert" class="error-text">Nu am putut încărca evenimentul. Încearcă din nou.</p>
    } @else if (event.value(); as current) {
      <h1 class="mt-4 mb-2 text-3xl font-bold">{{ current.name }}</h1>
      <p class="text-gray-700">{{ current.startsAt | date: 'EEEE, d MMMM y, HH:mm' }}</p>
      @if (current.status === 'CANCELLED') {
        <p role="alert" class="error-text font-semibold">Evenimentul a fost anulat.</p>
      }
      @if (current.description) {
        <p class="whitespace-pre-line">{{ current.description }}</p>
      }

      @if (venue.value(); as place) {
        <h2 class="mt-6 text-xl font-bold">Locația</h2>
        <p>
          {{ place.name }}<br />
          {{ place.address }}, {{ place.city }}
        </p>

        @if (place.sectors.length > 0) {
          <table class="mt-4 w-full border-collapse text-left">
            <caption class="sr-only">
              Sectoarele locației
            </caption>
            <thead>
              <tr class="border-b border-gray-300">
                <th scope="col" class="py-2">Sector</th>
                <th scope="col" class="py-2">Tip</th>
                <th scope="col" class="py-2 text-right">Locuri</th>
              </tr>
            </thead>
            <tbody>
              @for (sector of place.sectors; track sector.id) {
                <tr class="border-b border-gray-200">
                  <td class="py-2">{{ sector.name }}</td>
                  <td class="py-2">
                    {{ sector.kind === 'SEATED' ? 'Locuri numerotate' : 'În picioare' }}
                  </td>
                  <td class="py-2 text-right">{{ sector.capacity }}</td>
                </tr>
              }
            </tbody>
          </table>
          <p class="text-gray-700">Capacitate totală: {{ place.totalCapacity }} de locuri</p>
        }
      }

      <p class="mt-6 rounded bg-gray-100 p-3">Vânzarea biletelor va fi disponibilă în curând.</p>
    }
  `,
})
export class EventDetails {
  /** Filled by the router from /events/:id */
  readonly id = input.required<string>();

  protected readonly event = httpResource<EventSummary>(() => `/api/events/${this.id()}`);
  /** Starts only once the event is loaded and has a venue (undefined = no request). */
  protected readonly venue = httpResource<VenueDetails>(() => {
    const venueId = this.event.value()?.venueId;
    return venueId ? `/api/venues/${venueId}` : undefined;
  });

  protected readonly notFound = computed(() => {
    const error = this.event.error();
    return error instanceof HttpErrorResponse && error.status === 404;
  });
}
