import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

/**
 * Format a duration in minutes to "Xh Ym".
 */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

/**
 * Format a price in USD.
 */
export function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(price);
}

/**
 * Format an ISO-8601 datetime string to a readable local time.
 * E.g. "2025-06-01T08:00:00-04:00" → "8:00 AM"
 * Preserves the original timezone offset from the string.
 */
export function formatTime(isoString: string): string {
  const d = dayjs(isoString);
  return d.format('h:mm A');
}

/**
 * Format an ISO-8601 datetime string to "Jun 1" style.
 */
export function formatDate(isoString: string): string {
  return dayjs(isoString).format('MMM D');
}
