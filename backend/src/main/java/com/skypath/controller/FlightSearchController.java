package com.skypath.controller;

import com.skypath.dto.ErrorResponse;
import com.skypath.dto.SearchResponse;
import com.skypath.model.Itinerary;
import com.skypath.service.FlightDataService;
import com.skypath.service.FlightSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightSearchController {

    private final FlightSearchService searchService;
    private final FlightDataService dataService;

    public FlightSearchController(FlightSearchService searchService, FlightDataService dataService) {
        this.searchService = searchService;
        this.dataService = dataService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date
    ) {
        // --- Input Validation ---

        // Check required parameters are present
        if (origin == null || origin.isBlank()) {
            return badRequest("MISSING_ORIGIN", "The 'origin' parameter is required.");
        }
        if (destination == null || destination.isBlank()) {
            return badRequest("MISSING_DESTINATION", "The 'destination' parameter is required.");
        }
        if (date == null || date.isBlank()) {
            return badRequest("MISSING_DATE", "The 'date' parameter is required.");
        }

        String normalizedOrigin = origin.trim().toUpperCase();
        String normalizedDest = destination.trim().toUpperCase();

        if (!isValidIataCode(normalizedOrigin)) {
            return badRequest("INVALID_ORIGIN",
                    "Origin must be a 3-letter IATA airport code. Got: '" + origin + "'.");
        }
        if (!isValidIataCode(normalizedDest)) {
            return badRequest("INVALID_DESTINATION",
                    "Destination must be a 3-letter IATA airport code. Got: '" + destination + "'.");
        }

        if (!dataService.airportExists(normalizedOrigin)) {
            return badRequest("UNKNOWN_ORIGIN",
                    "Airport '" + normalizedOrigin + "' not found in the dataset.");
        }
        if (!dataService.airportExists(normalizedDest)) {
            return badRequest("UNKNOWN_DESTINATION",
                    "Airport '" + normalizedDest + "' not found in the dataset.");
        }

        if (normalizedOrigin.equals(normalizedDest)) {
            return badRequest("SAME_ORIGIN_DESTINATION",
                    "Origin and destination must be different airports.");
        }

        LocalDate searchDate;
        try {
            searchDate = LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            return badRequest("INVALID_DATE",
                    "Date must be in ISO 8601 format (YYYY-MM-DD). Got: '" + date + "'.");
        }

        List<Itinerary> itineraries = searchService.search(normalizedOrigin, normalizedDest, searchDate);

        return ResponseEntity.ok(new SearchResponse(itineraries, itineraries.size()));
    }

    private boolean isValidIataCode(String code) {
        return code != null && code.length() == 3 && code.chars().allMatch(Character::isUpperCase);
    }

    private ResponseEntity<ErrorResponse> badRequest(String error, String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(error, message, 400));
    }
}
