package com.skypath.model;

import java.util.List;

public record FlightDataset(
        List<Airport> airports,
        List<Flight> flights
) {
}
