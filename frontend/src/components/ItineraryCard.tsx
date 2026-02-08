import type { Itinerary } from '../types';
import { formatDuration, formatPrice, formatTime } from '../utils/format';

interface Props {
  itinerary: Itinerary;
}

function stopsLabel(stops: number): string {
  if (stops === 0) return 'Direct';
  if (stops === 1) return '1 Stop';
  return `${stops} Stops`;
}

function stopsBadgeClass(stops: number): string {
  if (stops === 0) return 'badge badge--direct';
  if (stops === 1) return 'badge badge--one-stop';
  return 'badge badge--two-stop';
}

export default function ItineraryCard({ itinerary }: Props) {
  const { segments, layovers, totalDurationMinutes, totalPrice, stops } =
    itinerary;

  return (
    <div className="itinerary-card">
      {/* Header */}
      <div className="itinerary-card__header">
        <span className={stopsBadgeClass(stops)}>{stopsLabel(stops)}</span>
        <span className="itinerary-card__duration">
          {formatDuration(totalDurationMinutes)}
        </span>
        <span className="itinerary-card__price">{formatPrice(totalPrice)}</span>
      </div>

      {/* Timeline */}
      <div className="itinerary-card__timeline">
        {segments.map((seg, i) => (
          <div key={seg.flightNumber + i}>
            {/* Segment */}
            <div className="segment">
              <div className="segment__airline">
                <span className="segment__airline-name">{seg.airline}</span>
                <span className="segment__flight-number">
                  {seg.flightNumber}
                </span>
                <span className="segment__aircraft">{seg.aircraft}</span>
              </div>

              <div className="segment__times">
                <div className="segment__departure">
                  <span className="segment__time">
                    {formatTime(seg.departureTime)}
                  </span>
                  <span className="segment__airport">
                    {seg.originCode}
                  </span>
                  <span className="segment__city">{seg.originCity}</span>
                </div>

                <div className="segment__arrow">
                  <span className="segment__duration">
                    {formatDuration(seg.durationMinutes)}
                  </span>
                  <div className="segment__arrow-line" />
                </div>

                <div className="segment__arrival">
                  <span className="segment__time">
                    {formatTime(seg.arrivalTime)}
                  </span>
                  <span className="segment__airport">
                    {seg.destinationCode}
                  </span>
                  <span className="segment__city">{seg.destinationCity}</span>
                </div>
              </div>
            </div>

            {/* Layover (between segments) */}
            {layovers[i] && (
              <div className="layover">
                <div className="layover__dot" />
                <div className="layover__info">
                  <span className="layover__duration">
                    {formatDuration(layovers[i].durationMinutes)} layover
                  </span>
                  <span className="layover__airport">
                    {layovers[i].airportCity} ({layovers[i].airportCode})
                  </span>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
