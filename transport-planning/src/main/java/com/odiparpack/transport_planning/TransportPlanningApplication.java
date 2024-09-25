package com.odiparpack.transport_planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import com.odiparpack.transport_planning.service.DataLoaderService;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            // Define date format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // Define start date: 1st May 2024
            Date startDate = null;
            Date endDate = null;
            try {
                startDate = sdf.parse("2024-05-01");
                endDate = sdf.parse("2024-05-07");
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Load data
            String citiesFilePath = "data/cities.txt";
            String roadSegmentsFilePath = "data/road_segments.txt";

            dataLoaderService.loadCities(citiesFilePath);
            dataLoaderService.loadRoadSegments(roadSegmentsFilePath);
            dataLoaderService.loadTrucksFromData();
            dataLoaderService.loadSalesData();

            // Run GLS algorithm with the defined date range
            glsAlgorithmService.runGLS(startDate, endDate);
        };
    }
}
