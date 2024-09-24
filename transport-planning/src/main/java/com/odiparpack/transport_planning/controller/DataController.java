package com.odiparpack.transport_planning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.odiparpack.transport_planning.service.DataLoaderService;

@RestController
@RequestMapping("/data")
public class DataController {

    @Autowired
    private DataLoaderService dataLoaderService;

    @PostMapping("/load")
    public String loadData() {
        try {
            // Specify the paths to your data files
            String citiesFilePath = "data/cities.txt";
            String roadSegmentsFilePath = "data/road_segments.txt";

            dataLoaderService.loadCities(citiesFilePath);
            dataLoaderService.loadRoadSegments(roadSegmentsFilePath);
            dataLoaderService.loadTrucksFromData();

            return "Data loaded successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error loading data: " + e.getMessage();
        }
    }

    @PostMapping("/loadSalesData")
    public String loadSalesData() {
        try {
            dataLoaderService.loadSalesData();
            return "Sales data loaded successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error loading sales data: " + e.getMessage();
        }
    }
}
