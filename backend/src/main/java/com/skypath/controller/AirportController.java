package com.skypath.controller;

import com.skypath.model.Airport;
import com.skypath.service.FlightDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final FlightDataService dataService;

    public AirportController(FlightDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Returns all airports in the dataset.
     * GET /api/airports
     */
    @GetMapping
    public Collection<Airport> getAllAirports() {
        return dataService.getAllAirports();
    }
}
