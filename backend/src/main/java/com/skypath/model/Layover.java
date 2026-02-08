package com.skypath.model;

public record Layover(
        String airportCode,
        String airportName,
        String airportCity,
        long durationMinutes
) {
}
