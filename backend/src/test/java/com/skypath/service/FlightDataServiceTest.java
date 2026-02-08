package com.skypath.service;

import com.skypath.model.Airport;
import com.skypath.model.Flight;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FlightDataService.
 * Loads the real flights.json from classpath and verifies data loading.
 */
@SpringBootTest
class FlightDataServiceTest {

    @Autowired
    private FlightDataService dataService;

    private static final LocalDate SEARCH_DATE = LocalDate.of(2024, 3, 15);

    @Test
    @DisplayName("Should load all 25 airports from flights.json")
    void loadAllAirports() {
        Collection<Airport> airports = dataService.getAllAirports();
        assertEquals(25, airports.size());
    }

    @Test
    @DisplayName("Should load JFK airport with correct properties")
    void loadJfkAirport() {
        Airport jfk = dataService.getAirport("JFK");
        assertNotNull(jfk);
        assertEquals("JFK", jfk.code());
        assertEquals("John F. Kennedy International", jfk.name());
        assertEquals("New York", jfk.city());
        assertEquals("US", jfk.country());
        assertEquals("America/New_York", jfk.timezone());
    }

    @Test
    @DisplayName("Should load SYD airport for date-line crossing tests")
    void loadSydAirport() {
        Airport syd = dataService.getAirport("SYD");
        assertNotNull(syd);
        assertEquals("AU", syd.country());
        assertEquals("Australia/Sydney", syd.timezone());
    }

    @Test
    @DisplayName("Should return true for known airports and false for unknown")
    void airportExistsCheck() {
        assertTrue(dataService.airportExists("JFK"));
        assertTrue(dataService.airportExists("LAX"));
        assertTrue(dataService.airportExists("NRT"));
        assertFalse(dataService.airportExists("XXX"));
        assertFalse(dataService.airportExists("ZZZ"));
        assertFalse(dataService.airportExists(""));
    }

    @Test
    @DisplayName("Should find flights from JFK on search date")
    void getFlightsByOriginAndDate() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE);
        assertFalse(flights.isEmpty());
        assertTrue(flights.stream().allMatch(f -> f.origin().equals("JFK")));
        assertTrue(flights.stream().allMatch(f ->
                f.departureTime().toLocalDate().equals(SEARCH_DATE)));
    }

    @Test
    @DisplayName("Should return empty list for origin with no flights on date")
    void noFlightsOnDifferentDate() {
        LocalDate otherDate = LocalDate.of(2024, 4, 1);
        List<Flight> flights = dataService.getFlightsByOriginAndDate("JFK", otherDate);
        assertTrue(flights.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for unknown origin")
    void noFlightsForUnknownOrigin() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("XXX", SEARCH_DATE);
        assertTrue(flights.isEmpty());
    }

    @Test
    @DisplayName("Should return all flights from origin regardless of date")
    void getFlightsByOrigin() {
        List<Flight> flights = dataService.getFlightsByOrigin("JFK");
        assertFalse(flights.isEmpty());
        assertTrue(flights.stream().allMatch(f -> f.origin().equals("JFK")));
    }

    @Test
    @DisplayName("Should find direct flights JFK -> LAX")
    void jfkToLaxDirectFlightsExist() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("JFK", SEARCH_DATE);
        long directToLax = flights.stream()
                .filter(f -> f.destination().equals("LAX"))
                .count();
        assertTrue(directToLax >= 3, "Should have at least 3 direct JFK->LAX flights");
    }

    @Test
    @DisplayName("Should find SYD -> LAX flights (date line crossing)")
    void sydToLaxFlightsExist() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("SYD", SEARCH_DATE);
        long toLax = flights.stream()
                .filter(f -> f.destination().equals("LAX"))
                .count();
        assertTrue(toLax >= 2, "Should have SYD->LAX flights");
    }

    @Test
    @DisplayName("Should find SFO -> NRT flights (international)")
    void sfoToNrtFlightsExist() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("SFO", SEARCH_DATE);
        long toNrt = flights.stream()
                .filter(f -> f.destination().equals("NRT"))
                .count();
        assertTrue(toNrt >= 2, "Should have SFO->NRT flights");
    }

    @Test
    @DisplayName("Should NOT have direct BOS -> SEA flights")
    void noDirectBosToSea() {
        List<Flight> flights = dataService.getFlightsByOriginAndDate("BOS", SEARCH_DATE);
        long directToSea = flights.stream()
                .filter(f -> f.destination().equals("SEA"))
                .count();
        assertEquals(0, directToSea, "There should be no direct BOS->SEA flights");
    }

    @Test
    @DisplayName("Should return null for non-existent airport code")
    void getNonExistentAirport() {
        assertNull(dataService.getAirport("XXX"));
        assertNull(dataService.getAirport("INVALID"));
    }
}
