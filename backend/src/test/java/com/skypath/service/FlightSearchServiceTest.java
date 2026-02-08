package com.skypath.service;

import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.model.Itinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FlightSearchService.
 * Uses Mockito to isolate the search algorithm from real flight data.
 */
@ExtendWith(MockitoExtension.class)
class FlightSearchServiceTest {

    @Mock
    private FlightDataService dataService;

    private FlightSearchService searchService;

    // Test airports
    private static final Airport JFK = new Airport("JFK", "JFK International", "New York", "US", "America/New_York");
    private static final Airport LAX = new Airport("LAX", "LAX International", "Los Angeles", "US", "America/Los_Angeles");
    private static final Airport ORD = new Airport("ORD", "O'Hare International", "Chicago", "US", "America/Chicago");
    private static final Airport DFW = new Airport("DFW", "DFW International", "Dallas", "US", "America/Chicago");
    private static final Airport LHR = new Airport("LHR", "London Heathrow", "London", "GB", "Europe/London");
    private static final Airport NRT = new Airport("NRT", "Narita International", "Tokyo", "JP", "Asia/Tokyo");
    private static final Airport SYD = new Airport("SYD", "Sydney Airport", "Sydney", "AU", "Australia/Sydney");

    private static final LocalDate SEARCH_DATE = LocalDate.of(2024, 3, 15);

    @BeforeEach
    void setUp() {
        searchService = new FlightSearchService(dataService);

        // Set up airport lookups (lenient to avoid unnecessary stubbing warnings)
        lenient().when(dataService.getAirport("JFK")).thenReturn(JFK);
        lenient().when(dataService.getAirport("LAX")).thenReturn(LAX);
        lenient().when(dataService.getAirport("ORD")).thenReturn(ORD);
        lenient().when(dataService.getAirport("DFW")).thenReturn(DFW);
        lenient().when(dataService.getAirport("LHR")).thenReturn(LHR);
        lenient().when(dataService.getAirport("NRT")).thenReturn(NRT);
        lenient().when(dataService.getAirport("SYD")).thenReturn(SYD);
    }

    // --- Helper to create flights ---
    private Flight flight(String num, String origin, String dest,
                          int depHour, int depMin, int arrHour, int arrMin, double price) {
        return flight(num, origin, dest, 15, depHour, depMin, 15, arrHour, arrMin, price);
    }

    private Flight flight(String num, String origin, String dest,
                          int depDay, int depHour, int depMin,
                          int arrDay, int arrHour, int arrMin, double price) {
        return new Flight(num, "TestAir", origin, dest,
                LocalDateTime.of(2024, 3, depDay, depHour, depMin),
                LocalDateTime.of(2024, 3, arrDay, arrHour, arrMin),
                price, "A320");
    }

    @Nested
    @DisplayName("Direct Flights")
    class DirectFlights {

        @Test
        @DisplayName("Should find direct flight when available")
        void findDirectFlight() {
            Flight direct = flight("F1", "JFK", "LAX", 8, 0, 11, 15, 299.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(direct));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            Itinerary it = results.get(0);
            assertEquals(0, it.stops());
            assertEquals(1, it.segments().size());
            assertEquals(0, it.layovers().size());
            assertEquals("JFK", it.segments().get(0).originCode());
            assertEquals("LAX", it.segments().get(0).destinationCode());
            assertEquals(299.0, it.totalPrice());
        }

        @Test
        @DisplayName("Should find multiple direct flights")
        void findMultipleDirectFlights() {
            Flight f1 = flight("F1", "JFK", "LAX", 8, 0, 11, 15, 299.0);
            Flight f2 = flight("F2", "JFK", "LAX", 14, 0, 17, 15, 329.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(f1, f2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(i -> i.stops() == 0));
        }

        @Test
        @DisplayName("Should return empty list when no flights exist")
        void noFlightsFound() {
            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(Collections.emptyList());

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("One-Stop Connections")
    class OneStopConnections {

        @Test
        @DisplayName("Should find 1-stop connection with valid domestic layover (>= 45 min)")
        void findOneStopDomesticConnection() {
            // JFK -> ORD (arrives 08:30 CT), then ORD -> LAX (departs 09:15 CT = 45 min layover)
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 9, 15, 11, 30, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            Itinerary it = results.get(0);
            assertEquals(1, it.stops());
            assertEquals(2, it.segments().size());
            assertEquals(1, it.layovers().size());
            assertEquals("ORD", it.layovers().get(0).airportCode());
            assertEquals(45, it.layovers().get(0).durationMinutes());
            assertEquals(414.0, it.totalPrice());
        }

        @Test
        @DisplayName("Should reject connection with domestic layover < 45 min")
        void rejectShortDomesticLayover() {
            // JFK -> ORD (arrives 08:30 CT), then ORD -> LAX (departs 09:00 CT = 30 min layover)
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 9, 0, 11, 15, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertTrue(results.isEmpty(), "Should reject connections with < 45 min domestic layover");
        }
    }

    @Nested
    @DisplayName("International Connection Rules")
    class InternationalConnections {

        @Test
        @DisplayName("Should require 90-min minimum layover for international connections")
        void requireInternationalMinimumLayover() {
            // JFK (US) -> LHR (GB), then LHR -> NRT (JP)
            // Arrival at LHR: 06:00 GMT, departure from LHR: 07:00 GMT = 60 min layover (should be rejected)
            Flight leg1 = flight("F1", "JFK", "LHR", 15, 18, 0, 16, 6, 0, 649.0);
            Flight leg2 = flight("F2", "LHR", "NRT", 7, 0, 16, 0, 849.0); // too soon, only 60 min

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("LHR"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "NRT", SEARCH_DATE);

            assertTrue(results.isEmpty(),
                    "Should reject international connections with < 90 min layover");
        }

        @Test
        @DisplayName("Should accept international connection with exactly 90-min layover")
        void acceptInternationalMinimumLayover() {
            // JFK (US) -> LHR (GB), then LHR -> NRT (JP)
            // Arrival at LHR: 06:00 GMT, departure from LHR: 07:30 GMT = 90 min layover (exactly minimum)
            Flight leg1 = flight("F1", "JFK", "LHR", 15, 18, 0, 16, 6, 0, 649.0);
            Flight leg2 = flight("F2", "LHR", "NRT", 16, 7, 30, 16, 16, 0, 849.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("LHR"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "NRT", SEARCH_DATE);

            assertEquals(1, results.size());
            assertEquals(1, results.get(0).stops());
            assertEquals(90, results.get(0).layovers().get(0).durationMinutes());
        }
    }

    @Nested
    @DisplayName("Max Layover Enforcement")
    class MaxLayover {

        @Test
        @DisplayName("Should reject connections exceeding 6-hour max layover")
        void rejectExcessiveLayover() {
            // JFK -> ORD (arrives 08:30 CT), ORD -> LAX (departs 15:00 CT = 390 min > 360 max)
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 15, 0, 17, 15, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertTrue(results.isEmpty(), "Should reject connections exceeding 6-hour max layover");
        }

        @Test
        @DisplayName("Should accept connection with exactly 360-min (6-hour) layover")
        void acceptMaxLayover() {
            // JFK -> ORD (arrives 08:30 CT), ORD -> LAX (departs 14:30 CT = 360 min exactly)
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 14, 30, 16, 45, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            assertEquals(360, results.get(0).layovers().get(0).durationMinutes());
        }
    }

    @Nested
    @DisplayName("Two-Stop Connections")
    class TwoStopConnections {

        @Test
        @DisplayName("Should find 2-stop connections")
        void findTwoStopConnection() {
            // JFK -> ORD -> DFW -> LAX
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);   // arr 08:30 CT
            Flight leg2 = flight("F2", "ORD", "DFW", 9, 30, 11, 45, 169.0);  // dep 09:30 CT (60 min layover)
            Flight leg3 = flight("F3", "DFW", "LAX", 14, 0, 15, 15, 179.0);  // dep 14:00 CT (135 min layover)

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));
            when(dataService.getFlightsByOrigin("DFW"))
                    .thenReturn(List.of(leg3));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            Itinerary it = results.get(0);
            assertEquals(2, it.stops());
            assertEquals(3, it.segments().size());
            assertEquals(2, it.layovers().size());
            assertEquals(537.0, it.totalPrice());
        }

        @Test
        @DisplayName("Should not exceed 2 stops (no 3-stop itineraries)")
        void noThreeStopConnections() {
            // JFK -> ORD -> DFW -> LAX -> SFO (would be 3 stops, should not happen)
            Airport SFO = new Airport("SFO", "SFO Airport", "San Francisco", "US", "America/Los_Angeles");
            lenient().when(dataService.getAirport("SFO")).thenReturn(SFO);

            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 100.0);
            Flight leg2 = flight("F2", "ORD", "DFW", 9, 30, 11, 0, 100.0);
            Flight leg3 = flight("F3", "DFW", "LAX", 12, 0, 13, 15, 100.0);
            Flight leg4 = flight("F4", "LAX", "SFO", 14, 0, 15, 0, 100.0); // this would be a 3rd stop

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));
            when(dataService.getFlightsByOrigin("DFW"))
                    .thenReturn(List.of(leg3));
            lenient().when(dataService.getFlightsByOrigin("LAX"))
                    .thenReturn(List.of(leg4));

            // Searching JFK -> SFO: should not find 3-stop path
            // The only path would be JFK->ORD->DFW->LAX->SFO (3 stops), which is too many
            List<Itinerary> results = searchService.search("JFK", "SFO", SEARCH_DATE);

            assertTrue(results.isEmpty(), "Should not allow more than 2 stops");
        }
    }

    @Nested
    @DisplayName("Cycle Prevention")
    class CyclePrevention {

        @Test
        @DisplayName("Should prevent visiting the same airport twice in a path")
        void preventCycles() {
            // JFK -> ORD -> JFK -> LAX (should not revisit JFK)
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight backToJFK = flight("F2", "ORD", "JFK", 9, 30, 12, 0, 195.0);
            Flight leg3 = flight("F3", "JFK", "LAX", 14, 0, 17, 15, 299.0);

            // Also provide a valid non-cyclic connection
            Flight ordToLax = flight("F4", "ORD", "LAX", 9, 15, 11, 30, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(backToJFK, ordToLax));
            lenient().when(dataService.getFlightsByOrigin("JFK"))
                    .thenReturn(List.of(leg3));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            // Should only find JFK->ORD->LAX, not JFK->ORD->JFK->LAX
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).stops());
            assertEquals("F4", results.get(0).segments().get(1).flightNumber());
        }
    }

    @Nested
    @DisplayName("Sorting and Ordering")
    class Sorting {

        @Test
        @DisplayName("Should sort results by total travel duration ascending")
        void sortByDuration() {
            // Two direct flights: one shorter, one longer
            Flight fast = flight("FAST", "JFK", "LAX", 8, 0, 11, 15, 399.0);   // ~6h15m (includes TZ)
            Flight slow = flight("SLOW", "JFK", "LAX", 14, 0, 18, 30, 199.0);  // ~7h30m

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(slow, fast)); // provide in reverse order

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(2, results.size());
            assertTrue(results.get(0).totalDurationMinutes() <= results.get(1).totalDurationMinutes(),
                    "Results should be sorted by total duration ascending");
        }
    }

    @Nested
    @DisplayName("Timezone-Aware Layover Calculation")
    class TimezoneHandling {

        @Test
        @DisplayName("Should correctly compute layover across timezone boundaries")
        void crossTimezoneLayover() {
            // JFK (EST -5) -> ORD (CST -6): arrives 08:30 CST
            // ORD -> LAX: departs 09:15 CST
            // Layover: 45 minutes in local ORD time
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 9, 15, 11, 30, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            // Arrival at ORD: 08:30 CST, Departure from ORD: 09:15 CST = 45 min
            assertEquals(45, results.get(0).layovers().get(0).durationMinutes());
        }

        @Test
        @DisplayName("Should reject connection where flight departs before previous one arrives (negative layover)")
        void rejectNegativeLayover() {
            // JFK -> ORD (arrives 12:30 CT), ORD -> LAX (departs 11:00 CT = negative)
            Flight leg1 = flight("F1", "JFK", "ORD", 11, 0, 12, 30, 189.0);
            Flight leg2 = flight("F2", "ORD", "LAX", 11, 0, 13, 15, 225.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertTrue(results.isEmpty(), "Should reject flights with negative layover");
        }
    }

    @Nested
    @DisplayName("Date Line Crossing")
    class DateLineCrossing {

        @Test
        @DisplayName("Should handle date line crossing where arrival time appears earlier than departure (SYD -> LAX)")
        void handleDateLineCrossing() {
            // SYD (AEDT +11) departs 09:00 Mar 15, arrives LAX (PST -8) at 06:00 Mar 15
            // Even though arrival LOCAL time is before departure LOCAL time, the UTC times are correct
            Flight dateLineFlight = flight("SYD1", "SYD", "LAX",
                    15, 9, 0, 15, 6, 0, 1099.0);

            when(dataService.getFlightsByOriginAndDate("SYD", SEARCH_DATE))
                    .thenReturn(List.of(dateLineFlight));

            List<Itinerary> results = searchService.search("SYD", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            Itinerary it = results.get(0);
            assertEquals(0, it.stops());
            // Duration should be positive and reasonable (about 16 hours)
            // SYD 09:00 AEDT = Mar 14 22:00 UTC, LAX 06:00 PST = Mar 15 14:00 UTC = 16 hours
            assertTrue(it.totalDurationMinutes() > 0,
                    "Duration should be positive even with date line crossing");
            assertTrue(it.totalDurationMinutes() < 1440,
                    "Duration should be less than 24 hours");
        }
    }

    @Nested
    @DisplayName("Itinerary Building")
    class ItineraryBuilding {

        @Test
        @DisplayName("Should correctly compute total price as sum of all segments")
        void correctTotalPrice() {
            Flight leg1 = flight("F1", "JFK", "ORD", 7, 0, 8, 30, 189.50);
            Flight leg2 = flight("F2", "ORD", "LAX", 9, 15, 11, 30, 225.75);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(leg1));
            when(dataService.getFlightsByOrigin("ORD"))
                    .thenReturn(List.of(leg2));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            assertEquals(415.25, results.get(0).totalPrice());
        }

        @Test
        @DisplayName("Should populate segment details correctly")
        void correctSegmentDetails() {
            Flight direct = flight("SP101", "JFK", "LAX", 8, 0, 11, 15, 299.0);

            when(dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE))
                    .thenReturn(List.of(direct));

            List<Itinerary> results = searchService.search("JFK", "LAX", SEARCH_DATE);

            assertEquals(1, results.size());
            var seg = results.get(0).segments().get(0);
            assertEquals("SP101", seg.flightNumber());
            assertEquals("TestAir", seg.airline());
            assertEquals("JFK", seg.originCode());
            assertEquals("JFK International", seg.originName());
            assertEquals("New York", seg.originCity());
            assertEquals("LAX", seg.destinationCode());
            assertEquals("LAX International", seg.destinationName());
            assertEquals("Los Angeles", seg.destinationCity());
            assertEquals("A320", seg.aircraft());
            assertTrue(seg.durationMinutes() > 0);
        }
    }
}
