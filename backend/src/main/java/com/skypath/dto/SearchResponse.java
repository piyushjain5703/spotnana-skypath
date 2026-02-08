package com.skypath.dto;

import com.skypath.model.Itinerary;
import java.util.List;

public record SearchResponse(
        List<Itinerary> itineraries,
        int count
) {
}
