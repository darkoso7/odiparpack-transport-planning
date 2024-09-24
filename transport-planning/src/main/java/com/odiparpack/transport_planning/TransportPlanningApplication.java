package com.odiparpack.transport_planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import com.odiparpack.transport_planning.service.DataLoaderService;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

@SpringBootApplication
public class TransportPlanningApplication {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    public static void main(String[] args) {
        SpringApplication.run(TransportPlanningApplication.class, args);
    }

    @Bean
    public ApplicationRunner initializer() {
        return args -> {
            // Load data
            String citiesFilePath = "data/cities.txt";
            String roadSegmentsFilePath = "data/road_segments.txt";

            dataLoaderService.loadCities(citiesFilePath);
            dataLoaderService.loadRoadSegments(roadSegmentsFilePath);
            dataLoaderService.loadTrucksFromData();
            dataLoaderService.loadSalesData();

            // Run GLS algorithm
            glsAlgorithmService.runGLS();
        };
    }
}
