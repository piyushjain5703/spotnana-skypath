package com.skypath.service;

import com.skypath.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private static final int MAX_STOPS = 2;
    private static final int MIN_LAYOVER_DOMESTIC_MINUTES = 45;
    private static final int MIN_LAYOVER_INTERNATIONAL_MINUTES = 90;
    private static final int MAX_LAYOVER_MINUTES = 360;

    private final FlightDataService dataService;

    public FlightSearchService(FlightDataService dataService) {
        this.dataService = dataService;
    }

    public List<Itinerary> search(String origin, String destination, LocalDate date) {
        List<Itinerary> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(origin);

        List<Flight> firstLegFlights = dataService.getFlightsByOriginAndDate(origin, date);
        log.debug("Found {} first-leg flights from {} on {}", firstLegFlights.size(), origin, date);

        for (Flight f1 : firstLegFlights) {
            List<Flight> path = new ArrayList<>();
            path.add(f1);

            if (f1.destination().equals(destination)) {
                results.add(buildItinerary(path));
            } else {
                visited.add(f1.destination());
                findConnections(path, destination, visited, results, 1);
                visited.remove(f1.destination());
            }
        }

        results.sort(Comparator.comparingLong(Itinerary::totalDurationMinutes));
        log.debug("Found {} total itineraries from {} to {} on {}", results.size(), origin, destination, date);
        return results;
    }

    private void findConnections(List<Flight> path, String dest,
                                 Set<String> visited, List<Itinerary> results, int depth) {
        if (depth > MAX_STOPS) {
            return;
        }

        Flight previousFlight = path.get(path.size() - 1);
        String currentAirport = previousFlight.destination();

        List<Flight> candidates = dataService.getFlightsByOrigin(currentAirport);

        for (Flight candidate : candidates) {
            if (!isValidConnection(previousFlight, candidate)) {
                continue;
            }

            if (visited.contains(candidate.destination()) && !candidate.destination().equals(dest)) {
                continue;
            }

            path.add(candidate);

            if (candidate.destination().equals(dest)) {
                results.add(buildItinerary(new ArrayList<>(path)));
            } else if (depth < MAX_STOPS) {
                visited.add(candidate.destination());
                findConnections(path, dest, visited, results, depth + 1);
                visited.remove(candidate.destination());
            }

            path.remove(path.size() - 1);
        }
    }

    private boolean isValidConnection(Flight arriving, Flight departing) {
        long layoverMinutes = calculateLayoverMinutes(arriving, departing);

        if (layoverMinutes < 0) {
            return false;
        }

        int minLayover = isDomesticConnection(arriving, departing)
                ? MIN_LAYOVER_DOMESTIC_MINUTES
                : MIN_LAYOVER_INTERNATIONAL_MINUTES;

        if (layoverMinutes < minLayover) {
            return false;
        }

        return layoverMinutes <= MAX_LAYOVER_MINUTES;
    }

    // Convert local airport times to UTC via each airport's timezone before computing duration
    private long calculateLayoverMinutes(Flight arriving, Flight departing) {
        Airport arrivalAirport = dataService.getAirport(arriving.destination());
        Airport departureAirport = dataService.getAirport(departing.origin());

        ZoneId arrivalTZ = ZoneId.of(arrivalAirport.timezone());
        ZoneId departureTZ = ZoneId.of(departureAirport.timezone());

        ZonedDateTime arrivalTime = arriving.arrivalTime().atZone(arrivalTZ);
        ZonedDateTime departureTime = departing.departureTime().atZone(departureTZ);

        return Duration.between(arrivalTime, departureTime).toMinutes();
    }

    // Connection is domestic only if BOTH flights are within the same country
    private boolean isDomesticConnection(Flight arriving, Flight departing) {
        String arrivingOriginCountry = dataService.getAirport(arriving.origin()).country();
        String arrivingDestCountry = dataService.getAirport(arriving.destination()).country();
        String departingOriginCountry = dataService.getAirport(departing.origin()).country();
        String departingDestCountry = dataService.getAirport(departing.destination()).country();

        boolean arrivingIsDomestic = arrivingOriginCountry.equals(arrivingDestCountry);
        boolean departingIsDomestic = departingOriginCountry.equals(departingDestCountry);

        return arrivingIsDomestic && departingIsDomestic;
    }

    private Itinerary buildItinerary(List<Flight> flights) {
        List<FlightSegment> segments = new ArrayList<>();
        List<Layover> layovers = new ArrayList<>();
        double totalPrice = 0.0;

        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            Airport origin = dataService.getAirport(f.origin());
            Airport dest = dataService.getAirport(f.destination());

            ZoneId originTZ = ZoneId.of(origin.timezone());
            ZoneId destTZ = ZoneId.of(dest.timezone());

            ZonedDateTime departure = f.departureTime().atZone(originTZ);
            ZonedDateTime arrival = f.arrivalTime().atZone(destTZ);
            long segmentDuration = Duration.between(departure, arrival).toMinutes();

            segments.add(new FlightSegment(
                    f.flightNumber(),
                    f.airline(),
                    f.origin(),
                    origin.name(),
                    origin.city(),
                    f.destination(),
                    dest.name(),
                    dest.city(),
                    departure.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    arrival.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    segmentDuration,
                    f.aircraft()
            ));

            totalPrice += f.price();

            if (i < flights.size() - 1) {
                Flight nextFlight = flights.get(i + 1);
                long layoverMinutes = calculateLayoverMinutes(f, nextFlight);

                layovers.add(new Layover(
                        f.destination(),
                        dest.name(),
                        dest.city(),
                        layoverMinutes
                ));
            }
        }

        Flight first = flights.get(0);
        Flight last = flights.get(flights.size() - 1);
        Airport firstOrigin = dataService.getAirport(first.origin());
        Airport lastDest = dataService.getAirport(last.destination());

        ZonedDateTime firstDeparture = first.departureTime()
                .atZone(ZoneId.of(firstOrigin.timezone()));
        ZonedDateTime lastArrival = last.arrivalTime()
                .atZone(ZoneId.of(lastDest.timezone()));
        long totalDuration = Duration.between(firstDeparture, lastArrival).toMinutes();

        return new Itinerary(
                segments,
                layovers,
                totalDuration,
                Math.round(totalPrice * 100.0) / 100.0, // avoid floating point artifacts
                flights.size() - 1
        );
    }
}
