import type { Itinerary } from '../types';
import ItineraryCard from './ItineraryCard';

interface Props {
  itineraries: Itinerary[];
}

export default function ResultsList({ itineraries }: Props) {
  return (
    <div className="results-list">
      <p className="results-list__count">
        {itineraries.length} itinerar{itineraries.length === 1 ? 'y' : 'ies'}{' '}
        found
      </p>
      <div className="results-list__items">
        {itineraries.map((it, i) => (
          <ItineraryCard key={i} itinerary={it} />
        ))}
      </div>
    </div>
  );
}
