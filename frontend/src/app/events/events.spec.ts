import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeRo from '@angular/common/locales/ro';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EventDetails } from './event-details';
import { EventList } from './event-list';

registerLocaleData(localeRo);

const venue = { id: 'v1', name: 'Arena Națională', city: 'București', address: 'Bd. Basarabia 37' };

function event(id: string, name: string, startsAt: string) {
  return { id, name, description: null, startsAt, status: 'PUBLISHED', venueId: 'v1' };
}

describe('events pages', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'ro' },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  it('lists the events soonest first, with their venue', async () => {
    const fixture = TestBed.createComponent(EventList);
    TestBed.tick();

    backend
      .expectOne('/api/events')
      .flush([
        event('2', 'Festival de vară', '2027-07-10T18:00:00Z'),
        event('1', 'Concert de primăvară', '2027-04-02T17:00:00Z'),
      ]);
    backend.expectOne('/api/venues').flush([venue]);
    await fixture.whenStable();

    const items = [...(fixture.nativeElement as HTMLElement).querySelectorAll('li')];
    expect(items.map((item) => item.querySelector('span')?.textContent?.trim())).toEqual([
      'Concert de primăvară',
      'Festival de vară',
    ]);
    expect(items[0].textContent).toContain('Arena Națională, București');
    expect(items[0].textContent).toContain('aprilie 2027');
  });

  it('says so when there are no events', async () => {
    const fixture = TestBed.createComponent(EventList);
    TestBed.tick();

    backend.expectOne('/api/events').flush([]);
    backend.expectOne('/api/venues').flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nu există evenimente');
  });

  it('shows the event with its venue and sectors', async () => {
    const fixture = TestBed.createComponent(EventDetails);
    fixture.componentRef.setInput('id', '1');
    TestBed.tick();

    backend
      .expectOne('/api/events/1')
      .flush(event('1', 'Concert de primăvară', '2027-04-02T17:00:00Z'));
    // the response reaches the resource one turn later; only then does the venue request start
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
    backend.expectOne('/api/venues/v1').flush({
      ...venue,
      totalCapacity: 1200,
      sectors: [
        { id: 's1', name: 'Tribuna A', kind: 'SEATED', capacity: 200 },
        { id: 's2', name: 'Gazon', kind: 'STANDING', capacity: 1000 },
      ],
    });
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Concert de primăvară');
    expect(text).toContain('Arena Națională');
    expect(text).toContain('Tribuna A');
    expect(text).toContain('În picioare');
    expect(text).toContain('Capacitate totală: 1200');
  });

  it('explains a missing event', async () => {
    const fixture = TestBed.createComponent(EventDetails);
    fixture.componentRef.setInput('id', 'nu-exista');
    TestBed.tick();

    backend
      .expectOne('/api/events/nu-exista')
      .flush({ code: 'EVENT_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Evenimentul nu există');
  });
});
