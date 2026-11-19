/** The shapes returned by /api/events and /api/venues (EventResponse, VenueResponse... in the backend). */

export interface EventSummary {
  id: string;
  name: string;
  description: string | null;
  startsAt: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED';
  venueId: string | null;
}

export interface Venue {
  id: string;
  name: string;
  city: string;
  address: string;
}

export interface Sector {
  id: string;
  name: string;
  kind: 'SEATED' | 'STANDING';
  capacity: number;
}

export interface VenueDetails extends Venue {
  totalCapacity: number;
  sectors: Sector[];
}
