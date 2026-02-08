package com.skypath.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.model.FlightDataset;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightDataService {

    private static final Logger log = LoggerFactory.getLogger(FlightDataService.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${skypath.data.path:classpath:flights.json}")
    private String dataPath;

    private Map<String, Airport> airportMap = Collections.emptyMap();

    private Map<String, List<Flight>> flightsByOrigin = Collections.emptyMap();

    public FlightDataService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadData() {
        Resource resource = resourceLoader.getResource(dataPath);
        if (!resource.exists()) {
            log.warn("flights.json not found at '{}'. Starting with empty dataset.", dataPath);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            FlightDataset dataset = objectMapper.readValue(is, FlightDataset.class);

            airportMap = dataset.airports().stream()
                    .collect(Collectors.toMap(Airport::code, a -> a));

            flightsByOrigin = dataset.flights().stream()
                    .collect(Collectors.groupingBy(Flight::origin));

            log.info("Loaded {} airports and {} flights.", airportMap.size(), dataset.flights().size());
        } catch (IOException e) {
            log.error("Failed to load flights.json from '{}': {}", dataPath, e.getMessage(), e);
        }
    }

    public Airport getAirport(String code) {
        return airportMap.get(code);
    }

    public boolean airportExists(String code) {
        return airportMap.containsKey(code);
    }

    // Filters by local departure date
    public List<Flight> getFlightsByOriginAndDate(String origin, LocalDate date) {
        return flightsByOrigin.getOrDefault(origin, Collections.emptyList()).stream()
                .filter(f -> f.departureTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    // Returns all flights from origin; layover time constraints handle temporal filtering
    public List<Flight> getFlightsByOrigin(String origin) {
        return flightsByOrigin.getOrDefault(origin, Collections.emptyList());
    }

    public Collection<Airport> getAllAirports() {
        return airportMap.values();
    }
}
