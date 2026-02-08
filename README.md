# SkyPath: Flight Connection Search Engine

A full-stack flight connection search engine that finds valid itineraries (direct, 1-stop, 2-stop) between airports with timezone-aware connection rules. Built with a Java Spring Boot REST backend and React TypeScript frontend, fully Dockerized via docker-compose.

---

## Quick Start

### Using Docker (Recommended)

```bash
docker compose up --build
```

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080

The backend healthcheck runs automatically. The frontend container waits until the backend is healthy before starting.

### Local Development

**Backend** (requires Java 17+):
```bash
cd backend
./gradlew bootRun
```

**Frontend** (requires Node.js 20+):
```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `localhost:8080`.

### Running Tests

```bash
cd backend
./gradlew test
```

This runs **61 tests** covering unit tests, integration tests, and all 6 assignment test cases.

---

## Project Structure

```
spotnana-skypath/
├── backend/                          # Java Spring Boot REST API
│   ├── src/main/java/com/skypath/
│   │   ├── SkypathBackendApplication.java    # Main entry point
│   │   ├── config/WebConfig.java             # CORS configuration
│   │   ├── controller/
│   │   │   ├── FlightSearchController.java   # GET /api/flights/search
│   │   │   └── AirportController.java        # GET /api/airports
│   │   ├── dto/
│   │   │   ├── SearchResponse.java           # Itineraries + count
│   │   │   └── ErrorResponse.java            # Structured error
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java   # Centralized error handling
│   │   ├── model/
│   │   │   ├── Airport.java                  # Airport record (code, timezone, etc.)
│   │   │   ├── Flight.java                   # Flight record (times, price, etc.)
│   │   │   ├── FlightDataset.java            # Root JSON structure
│   │   │   ├── FlightSegment.java            # Enriched segment in response
│   │   │   ├── Itinerary.java                # Complete itinerary
│   │   │   └── Layover.java                  # Layover info between segments
│   │   └── service/
│   │       ├── FlightDataService.java        # Data loader + in-memory index
│   │       └── FlightSearchService.java      # DFS search algorithm + rules
│   ├── src/main/resources/
│   │   ├── application.yml                   # Server config
│   │   └── flights.json                      # 25 airports, 303 flights
│   ├── src/test/java/com/skypath/
│   │   ├── service/
│   │   │   ├── FlightSearchServiceTest.java  # 16 unit tests (mocked)
│   │   │   └── FlightDataServiceTest.java    # 13 integration tests
│   │   └── controller/
│   │       └── FlightSearchControllerTest.java # 31 MockMvc tests
│   ├── Dockerfile                            # Multi-stage JDK/JRE build
│   ├── build.gradle
│   └── settings.gradle
├── frontend/                         # React + TypeScript + Vite
│   ├── src/
│   │   ├── components/
│   │   │   ├── SearchForm.tsx                # Origin/destination/date inputs
│   │   │   ├── ResultsList.tsx               # Itinerary list container
│   │   │   ├── ItineraryCard.tsx             # Visual itinerary timeline
│   │   │   ├── LoadingSpinner.tsx            # Animated loading state
│   │   │   ├── EmptyState.tsx                # No results message
│   │   │   └── ErrorBanner.tsx               # Dismissable error alert
│   │   ├── services/api.ts                   # Axios HTTP client
│   │   ├── types/index.ts                    # TypeScript interfaces
│   │   ├── utils/format.ts                   # Duration/price formatters
│   │   ├── App.tsx                           # Root component
│   │   ├── App.css                           # Component styles
│   │   └── index.css                         # Global styles
│   ├── Dockerfile                            # Multi-stage Node/Nginx build
│   ├── nginx.conf                            # Reverse proxy + SPA config
│   ├── vite.config.ts
│   └── package.json
├── docker-compose.yml                # Two-service orchestration
├── flights.json                      # Source data file
├── PLAN.md                           # Implementation plan
└── README.md                         # This file
```

---

## API Reference

### Search Flights

```
GET /api/flights/search?origin={IATA}&destination={IATA}&date={YYYY-MM-DD}
```

**Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `origin` | string | 3-letter IATA airport code (e.g., `JFK`) |
| `destination` | string | 3-letter IATA airport code (e.g., `LAX`) |
| `date` | string | ISO 8601 date (e.g., `2024-03-15`) |

**Success Response (200):**
```json
{
  "itineraries": [
    {
      "segments": [
        {
          "flightNumber": "SP101",
          "airline": "SkyPath Airways",
          "originCode": "JFK",
          "originName": "John F. Kennedy International",
          "originCity": "New York",
          "destinationCode": "LAX",
          "destinationName": "Los Angeles International",
          "destinationCity": "Los Angeles",
          "departureTime": "2024-03-15T08:30:00-05:00",
          "arrivalTime": "2024-03-15T11:45:00-08:00",
          "durationMinutes": 375,
          "aircraft": "A320"
        }
      ],
      "layovers": [],
      "totalDurationMinutes": 375,
      "totalPrice": 299.00,
      "stops": 0
    }
  ],
  "count": 1
}
```

**Error Response (400):**
```json
{
  "error": "UNKNOWN_ORIGIN",
  "message": "Airport 'XXX' not found in the dataset.",
  "statusCode": 400
}
```

### List Airports

```
GET /api/airports
```

Returns all 25 airports in the dataset.

---

## Architecture Decisions

### 1. Spring Boot (Java 17) for the Backend

**Why:** Spring Boot provides a mature, production-ready framework with built-in JSON serialization (Jackson), dependency injection, and excellent testing support. Java records (introduced in Java 16) give us immutable, concise data models. The `ZonedDateTime` API provides first-class timezone support essential for correct layover calculations.

**Alternative considered:** Node.js/Express would have been lighter weight, but Java's type system and timezone libraries (`java.time`) are more robust for the precision needed in flight scheduling math.

### 2. In-Memory Data Store with HashMap-Based Adjacency List

**Why:** With only 25 airports and ~303 flights, a database would add operational complexity with no performance benefit. The data is loaded once at startup into two maps:
- `Map<String, Airport>` -- O(1) airport lookup by IATA code
- `Map<String, List<Flight>>` -- O(1) flight lookup by origin airport (adjacency list)

This gives us constant-time access to all neighbors of any airport, which is the primary operation in the search algorithm.

**Tradeoff:** If the dataset grew to thousands of airports and millions of flights, we'd need a database with proper indexing. The in-memory approach won't scale to production airline data.

### 3. DFS with Backtracking (Max Depth 3)

**Why:** The search uses depth-first search with backtracking up to depth 3 (max 2 stops = 3 flight segments). DFS is chosen over BFS because:
- With a small depth limit (3), DFS and BFS have similar performance
- DFS naturally backtracks, making it simple to build paths and enforce constraints at each step
- Memory usage is O(depth) vs O(branching_factor^depth) for BFS

The algorithm tracks visited airports via a set with backtracking to prevent cycles (e.g., JFK -> ORD -> JFK -> LAX would revisit JFK).

**Tradeoff:** DFS doesn't guarantee shortest-path-first exploration, but since we sort results by total duration anyway, this doesn't affect the final output.

### 4. Timezone-Aware Layover Calculation

**Why:** Layover duration is computed by converting both arrival and departure times to UTC-aware `ZonedDateTime` objects using each airport's IANA timezone, then computing `Duration.between()`. This correctly handles:
- **Cross-timezone connections** (e.g., JFK EST -> ORD CST): layover computed in real elapsed time, not local clock difference
- **Date line crossing** (e.g., SYD AEDT -> LAX PST): arrival local time appears "before" departure local time, but UTC comparison gives the correct ~16-hour duration
- **DST transitions**: `ZoneId.of()` with `java.time` handles DST automatically

**Alternative considered:** Converting everything to UTC epoch millis would also work but lose readability. Using `ZonedDateTime` preserves the local-time semantics for display while enabling correct duration math.

### 5. Connection Rule Implementation

Connections are validated with three rules applied in order:

| Rule | Domestic | International |
|------|----------|---------------|
| Minimum layover | 45 minutes | 90 minutes |
| Maximum layover | 6 hours (360 min) | 6 hours (360 min) |

A connection is **domestic** only if **both** the arriving flight and the departing flight are within the same country (both origin and destination of each flight share the same country code). This means a US domestic flight connecting to an international flight at a US hub is treated as an international connection, requiring the 90-minute minimum.

### 6. Nginx Reverse Proxy (Frontend)

**Why:** The production frontend is served via Nginx with a reverse proxy configuration that routes `/api/` requests to the backend container. This avoids CORS issues entirely in production since all requests come from the same origin.

In development, Vite's built-in proxy provides the same behavior.

### 7. React + TypeScript + Vite (Frontend)

**Why:** React for component-based UI, TypeScript for type safety mirroring backend response types, and Vite for fast development builds. No heavy UI framework (Material UI, etc.) was used -- the UI is built with plain CSS using BEM naming conventions for simplicity and bundle size.

### 8. Multi-Stage Docker Builds

**Why:** Keeps final images small by separating build dependencies from runtime:
- **Backend**: Build with JDK 17 (~400MB), run with JRE 17 (~200MB)
- **Frontend**: Build with Node 20 (~1GB), serve with Nginx Alpine (~40MB)

Used `jammy` (Ubuntu) instead of Alpine for the JRE image to ensure ARM64/Apple Silicon compatibility.

---

## Test Coverage

### Unit Tests (FlightSearchServiceTest) -- 16 tests

Tests the search algorithm in isolation using Mockito mocks for the data service:

| Category | Tests | What's Tested |
|----------|-------|---------------|
| Direct Flights | 3 | Find single/multiple directs, empty results |
| One-Stop Connections | 2 | Valid 45-min domestic layover, reject <45 min |
| International Rules | 2 | Reject <90 min international, accept exactly 90 min |
| Max Layover | 2 | Reject >360 min, accept exactly 360 min |
| Two-Stop Connections | 2 | Find 2-stop paths, enforce no 3-stop paths |
| Cycle Prevention | 1 | Prevent revisiting origin airport |
| Sorting | 1 | Results sorted by duration ascending |
| Timezone Handling | 2 | Cross-timezone layover math, negative layover rejection |
| Date Line Crossing | 1 | SYD->LAX positive duration despite local time paradox |
| Itinerary Building | 2 | Correct total price, segment detail population |

### Integration Tests (FlightDataServiceTest) -- 13 tests

Tests data loading with the real `flights.json`:
- All 25 airports loaded with correct properties
- Airport existence checks (known/unknown)
- Flight queries by origin, by origin+date
- Verification of specific route data (JFK->LAX directs, no BOS->SEA directs, SYD->LAX exists)

### Controller Tests (FlightSearchControllerTest) -- 31 tests

Full integration tests using Spring MockMvc covering all 6 assignment test cases:

| Test Case | Tests | Scenario |
|-----------|-------|----------|
| JFK -> LAX | 6 | Direct + multi-stop, sorted, correct origin/dest, layover limits |
| SFO -> NRT | 3 | International flights, layover minimums, positive durations |
| BOS -> SEA | 4 | No directs, connections only, first segment from BOS |
| JFK -> JFK | 1 | Same origin/destination returns 400 error |
| XXX -> LAX | 2 | Unknown origin/destination returns 400 error |
| SYD -> LAX | 4 | Date line crossing, positive durations, reasonable flight times |
| Input Validation | 8 | Missing params, invalid IATA codes, bad dates, case/whitespace handling |
| Airport Endpoint | 2 | Returns all 25 airports with required fields |

---

## What I'd Improve

### Performance & Scalability
- **Database integration**: Replace in-memory store with PostgreSQL + proper indexing for large-scale datasets
- **Caching**: Add Redis/Caffeine cache for frequently searched routes (e.g., JFK-LAX) with TTL-based invalidation
- **Pagination**: Return results in pages instead of all at once for routes with many itineraries
- **Async search**: For very large datasets, use CompletableFuture or reactive streams to parallelize DFS branches

### Features
- **Airport autocomplete**: Replace text input with typeahead search using real IATA database (airport name, city, code)
- **Filtering & sorting**: Allow users to filter by number of stops, max price, preferred airline; sort by price, duration, or departure time
- **Flexible dates**: Search across +/- 3 days to find better prices
- **Price calendar**: Show lowest price per day in a calendar view
- **Seat class**: Support economy/business/first class pricing tiers

### Testing
- **Frontend unit tests**: Add React Testing Library tests for components (SearchForm validation, ItineraryCard rendering)
- **E2E tests**: Add Cypress/Playwright tests for full user flows
- **Load testing**: Use JMeter/k6 to stress-test the search endpoint with concurrent requests
- **Property-based testing**: Use jqwik to generate random flight networks and verify algorithm invariants

### Infrastructure
- **CI/CD pipeline**: GitHub Actions for automated test runs, Docker image builds, and deployment
- **Health monitoring**: Add Prometheus metrics and Grafana dashboards for search latency, error rates
- **API versioning**: Prefix endpoints with `/v1/` for backward-compatible evolution
- **Rate limiting**: Add request throttling to prevent abuse of the search endpoint
- **OpenAPI/Swagger**: Auto-generate API documentation from annotations

---

## Dataset

The application ships with a curated dataset of **25 airports** across 12 countries and **303 flights** operated by SkyPath Airways, all on March 15, 2024. The dataset includes:

- **Domestic US routes**: Hub-and-spoke network connecting JFK, LAX, ORD, DFW, ATL, DEN, SEA, SFO, BOS, MIA, PHX, LGA, EWR
- **Transatlantic routes**: JFK/YYZ to LHR, CDG, FRA, AMS
- **Transpacific routes**: LAX/SFO to NRT, HND, SYD
- **Asian routes**: SIN, HKG, NRT interconnections
- **Middle East**: DXB hub connections to LHR, SIN, JFK
- **Americas**: YYZ (Canada), MEX (Mexico) connections

This dataset was designed to exercise all search scenarios: direct flights, multi-stop domestic connections, international connections with longer minimum layovers, date-line crossing flights (SYD-LAX), and routes with no direct service (BOS-SEA).
