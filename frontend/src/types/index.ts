/* ------------------------------------------------------------------ */
/*  TypeScript types mirroring the Java backend response models       */
/* ------------------------------------------------------------------ */

export interface FlightSegment {
  flightNumber: string;
  airline: string;
  originCode: string;
  originName: string;
  originCity: string;
  destinationCode: string;
  destinationName: string;
  destinationCity: string;
  departureTime: string; // ISO-8601 with offset, e.g. "2025-06-01T08:00:00-04:00"
  arrivalTime: string;
  durationMinutes: number;
  aircraft: string;
}

export interface Layover {
  airportCode: string;
  airportName: string;
  airportCity: string;
  durationMinutes: number;
}

export interface Itinerary {
  segments: FlightSegment[];
  layovers: Layover[];
  totalDurationMinutes: number;
  totalPrice: number;
  stops: number;
}

export interface SearchResponse {
  itineraries: Itinerary[];
  count: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  statusCode: number;
}

export interface Airport {
  code: string;
  name: string;
  city: string;
  country: string;
  timezone: string;
}
