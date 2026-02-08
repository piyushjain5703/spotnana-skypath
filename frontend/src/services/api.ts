import axios, { AxiosError } from 'axios';
import type { SearchResponse, ErrorResponse, Airport } from '../types';

const API_BASE = import.meta.env.VITE_API_URL ?? '';

const client = axios.create({
  baseURL: API_BASE,
  timeout: 15_000,
});

/**
 * Search for flight itineraries.
 */
export async function searchFlights(
  origin: string,
  destination: string,
  date: string,
): Promise<SearchResponse> {
  try {
    const { data } = await client.get<SearchResponse>('/api/flights/search', {
      params: { origin, destination, date },
    });
    return data;
  } catch (err) {
    throw toUserError(err);
  }
}

/**
 * Fetch all airports (for autocomplete / validation hints).
 */
export async function fetchAirports(): Promise<Airport[]> {
  try {
    const { data } = await client.get<Airport[]>('/api/airports');
    return data;
  } catch (err) {
    throw toUserError(err);
  }
}

/* ------------------------------------------------------------------ */
/*  Error mapping                                                      */
/* ------------------------------------------------------------------ */

export class ApiError extends Error {
  code: string;
  statusCode: number;

  constructor(code: string, message: string, statusCode: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.statusCode = statusCode;
  }
}

function toUserError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<ErrorResponse>;

    // Server returned a structured error body
    if (axiosErr.response?.data?.error) {
      const { error, message, statusCode } = axiosErr.response.data;
      return new ApiError(error, message, statusCode);
    }

    // Network / timeout errors
    if (axiosErr.code === 'ECONNABORTED') {
      return new ApiError('TIMEOUT', 'The request timed out. Please try again.', 0);
    }
    if (!axiosErr.response) {
      return new ApiError(
        'NETWORK_ERROR',
        'Unable to reach the server. Please check your connection and try again.',
        0,
      );
    }

    // Fallback for unstructured server errors
    return new ApiError(
      'SERVER_ERROR',
      `Server returned ${axiosErr.response.status}. Please try again later.`,
      axiosErr.response.status,
    );
  }

  return new ApiError('UNKNOWN', 'An unexpected error occurred.', 0);
}
