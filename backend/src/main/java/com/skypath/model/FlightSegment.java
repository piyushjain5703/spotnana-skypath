package com.skypath.model;

public record FlightSegment(
        String flightNumber,
        String airline,
        String originCode,
        String originName,
        String originCity,
        String destinationCode,
        String destinationName,
        String destinationCity,
        String departureTime,
        String arrivalTime,
        long durationMinutes,
        String aircraft
) {
}
