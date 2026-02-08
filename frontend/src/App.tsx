import { useState, useCallback } from 'react';
import SearchForm from './components/SearchForm';
import ResultsList from './components/ResultsList';
import LoadingSpinner from './components/LoadingSpinner';
import EmptyState from './components/EmptyState';
import ErrorBanner from './components/ErrorBanner';
import { searchFlights, ApiError } from './services/api';
import type { Itinerary } from './types';
import './App.css';

type SearchState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; itineraries: Itinerary[]; origin: string; destination: string }
  | { status: 'error'; message: string };

export default function App() {
  const [state, setState] = useState<SearchState>({ status: 'idle' });

  const handleSearch = useCallback(
    async (origin: string, destination: string, date: string) => {
      setState({ status: 'loading' });
      try {
        const response = await searchFlights(origin, destination, date);
        setState({
          status: 'success',
          itineraries: response.itineraries,
          origin,
          destination,
        });
      } catch (err) {
        const message =
          err instanceof ApiError
            ? err.message
            : 'An unexpected error occurred.';
        setState({ status: 'error', message });
      }
    },
    [],
  );

  return (
    <div className="app">
      {/* Header */}
      <header className="app__header">
        <div className="app__header-inner">
          <h1 className="app__logo">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              width="28"
              height="28"
            >
              <path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z" />
            </svg>
            SkyPath
          </h1>
          <p className="app__tagline">Find the best flight connections</p>
        </div>
      </header>

      {/* Main */}
      <main className="app__main">
        <SearchForm
          onSearch={handleSearch}
          loading={state.status === 'loading'}
        />

        <div className="app__results">
          {state.status === 'loading' && <LoadingSpinner />}

          {state.status === 'error' && (
            <ErrorBanner
              message={state.message}
              onDismiss={() => setState({ status: 'idle' })}
            />
          )}

          {state.status === 'success' &&
            (state.itineraries.length > 0 ? (
              <ResultsList itineraries={state.itineraries} />
            ) : (
              <EmptyState
                origin={state.origin}
                destination={state.destination}
              />
            ))}
        </div>
      </main>

      {/* Footer */}
      <footer className="app__footer">
        <p>
          SkyPath Flight Search &mdash; Built with Spring Boot &amp; React
          &nbsp;by{' '}
          <a
            href="https://linktr.ee/piyushjain5703"
            target="_blank"
            rel="noopener noreferrer"
            className="app__footer-link"
          >
            Piyush Jain
          </a>
        </p>
      </footer>
    </div>
  );
}
