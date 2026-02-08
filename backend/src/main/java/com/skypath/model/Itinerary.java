package com.skypath.model;

import java.util.List;

public record Itinerary(
        List<FlightSegment> segments,
        List<Layover> layovers,
        long totalDurationMinutes,
        double totalPrice,
        int stops
) {
}
