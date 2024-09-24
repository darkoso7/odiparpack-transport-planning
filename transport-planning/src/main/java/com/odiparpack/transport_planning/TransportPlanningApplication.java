package com.odiparpack.transport_planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.odiparpack.transport_planning.service.DataLoaderService;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

@SpringBootApplication
public class TransportPlanningApplication {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    @Autowired
    private ResourceLoader resourceLoader;

    public static void main(String[] args) {
        SpringApplication.run(TransportPlanningApplication.class, args);
    }

    @Bean
    public ApplicationRunner initializer() {
        return args -> {
            // Load data from classpath resources
            Resource citiesFileResource = resourceLoader.getResource("classpath:data/cities.txt");
            Resource roadSegmentsFileResource = resourceLoader.getResource("classpath:data/road_segments.txt");


            dataLoaderService.loadCities(citiesFileResource.getFile().getPath());
            dataLoaderService.loadRoadSegments(roadSegmentsFileResource.getFile().getPath());
            dataLoaderService.loadTrucksFromData();
            dataLoaderService.loadSalesData();

            // Run GLS algorithm
            glsAlgorithmService.runGLS();
        };
    }
}
