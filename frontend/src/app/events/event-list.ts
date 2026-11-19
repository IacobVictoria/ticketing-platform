import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventSummary, Venue } from './event.model';

/** The home page: published events, soonest first. */
@Component({
  selector: 'app-event-list',
  imports: [DatePipe, RouterLink],
  template: `
    <h1 class="mb-4 text-3xl font-bold">Evenimente</h1>

    @if (events.isLoading()) {
      <p role="status">Se încarcă evenimentele...</p>
    } @else if (events.error()) {
      <p role="alert" class="error-text">Nu am putut încărca evenimentele. Încearcă din nou.</p>
    } @else if (sortedEvents().length === 0) {
      <p>Nu există evenimente momentan.</p>
    } @else {
      <ul class="mt-4 space-y-3">
        @for (event of sortedEvents(); track event.id) {
          <li>
            <a
              [routerLink]="['/events', event.id]"
              class="block rounded border border-gray-300 p-4 hover:border-blue-700 hover:bg-blue-50"
            >
              <span class="block text-lg font-semibold text-blue-700">{{ event.name }}</span>
              <span class="block text-gray-700">{{
                event.startsAt | date: 'd MMMM y, HH:mm'
              }}</span>
              @if (venueName(event.venueId); as name) {
                <span class="block text-gray-700">{{ name }}</span>
              }
            </a>
          </li>
        }
      </ul>
    }
  `,
})
export class EventList {
  protected readonly events = httpResource<EventSummary[]>(() => '/api/events', {
    defaultValue: [],
  });
  protected readonly venues = httpResource<Venue[]>(() => '/api/venues', { defaultValue: [] });

  protected readonly sortedEvents = computed(() =>
    [...this.events.value()].sort((a, b) => a.startsAt.localeCompare(b.startsAt)),
  );

  /** id → "Arena Națională, București"; the event list only carries the venue's id. */
  private readonly venueNames = computed(
    () => new Map(this.venues.value().map((venue) => [venue.id, `${venue.name}, ${venue.city}`])),
  );

  protected venueName(venueId: string | null): string | undefined {
    return venueId ? this.venueNames().get(venueId) : undefined;
  }
}
