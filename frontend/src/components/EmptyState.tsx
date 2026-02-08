interface Props {
  origin: string;
  destination: string;
}

export default function EmptyState({ origin, destination }: Props) {
  return (
    <div className="empty-state">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        width="48"
        height="48"
      >
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
        <line x1="8" y1="11" x2="14" y2="11" />
      </svg>
      <h3>No flights found</h3>
      <p>
        We couldn't find any itineraries from <strong>{origin}</strong> to{' '}
        <strong>{destination}</strong> on this date. Try a different date or
        route.
      </p>
    </div>
  );
}
