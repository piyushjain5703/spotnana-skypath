import { useState, useCallback, useEffect } from 'react';
import type { Airport } from '../types';
import { fetchAirports } from '../services/api';

interface Props {
  onSearch: (origin: string, destination: string, date: string) => void;
  loading: boolean;
}

export default function SearchForm({ onSearch, loading }: Props) {
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [date, setDate] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [airports, setAirports] = useState<Airport[]>([]);

  useEffect(() => {
    fetchAirports()
      .then(setAirports)
      .catch(() => {});
  }, []);

  const airportCodes = new Set(airports.map((a) => a.code));

  const validate = useCallback((): boolean => {
    const e: Record<string, string> = {};

    if (!origin.trim()) {
      e.origin = 'Origin is required';
    } else if (!/^[A-Z]{3}$/.test(origin.trim())) {
      e.origin = 'Enter a valid 3-letter airport code';
    } else if (airports.length > 0 && !airportCodes.has(origin.trim())) {
      e.origin = `Airport "${origin.trim()}" not found`;
    }

    if (!destination.trim()) {
      e.destination = 'Destination is required';
    } else if (!/^[A-Z]{3}$/.test(destination.trim())) {
      e.destination = 'Enter a valid 3-letter airport code';
    } else if (airports.length > 0 && !airportCodes.has(destination.trim())) {
      e.destination = `Airport "${destination.trim()}" not found`;
    }

    if (
      origin.trim() &&
      destination.trim() &&
      origin.trim() === destination.trim()
    ) {
      e.destination = 'Destination must differ from origin';
    }

    if (!date) {
      e.date = 'Date is required';
    }

    setErrors(e);
    return Object.keys(e).length === 0;
  }, [origin, destination, date, airports, airportCodes]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      onSearch(origin.trim(), destination.trim(), date);
    }
  };

  const handleAirportInput =
    (setter: (v: string) => void) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setter(e.target.value.toUpperCase().slice(0, 3));
      setErrors({});
    };

  return (
    <form className="search-form" onSubmit={handleSubmit}>
      <div className="search-form__row">
        {/* Origin */}
        <div className="search-form__field">
          <label htmlFor="origin">From</label>
          <input
            id="origin"
            type="text"
            placeholder="JFK"
            value={origin}
            onChange={handleAirportInput(setOrigin)}
            maxLength={3}
            autoComplete="off"
          />
          {errors.origin && (
            <span className="search-form__error">{errors.origin}</span>
          )}
        </div>

        {/* Swap button */}
        <button
          type="button"
          className="search-form__swap"
          title="Swap origin and destination"
          onClick={() => {
            setOrigin(destination);
            setDestination(origin);
            setErrors({});
          }}
          aria-label="Swap origin and destination"
        >
          &#8646;
        </button>

        {/* Destination */}
        <div className="search-form__field">
          <label htmlFor="destination">To</label>
          <input
            id="destination"
            type="text"
            placeholder="LAX"
            value={destination}
            onChange={handleAirportInput(setDestination)}
            maxLength={3}
            autoComplete="off"
          />
          {errors.destination && (
            <span className="search-form__error">{errors.destination}</span>
          )}
        </div>

        {/* Date */}
        <div className="search-form__field">
          <label htmlFor="date">Date</label>
          <input
            id="date"
            type="date"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              setErrors({});
            }}
          />
          {errors.date && (
            <span className="search-form__error">{errors.date}</span>
          )}
        </div>

        {/* Submit */}
        <button
          type="submit"
          className="search-form__submit"
          disabled={loading}
        >
          {loading ? 'Searching…' : 'Search Flights'}
        </button>
      </div>
    </form>
  );
}
