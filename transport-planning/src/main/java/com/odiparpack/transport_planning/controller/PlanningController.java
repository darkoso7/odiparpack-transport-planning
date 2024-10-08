package com.odiparpack.transport_planning.controller;

import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.odiparpack.transport_planning.model.City;
import com.odiparpack.transport_planning.model.PackageOrder;
import com.odiparpack.transport_planning.model.RoadSegment;
import com.odiparpack.transport_planning.model.Truck;
import com.odiparpack.transport_planning.repository.CityRepository;
import com.odiparpack.transport_planning.repository.PackageOrderRepository;
import com.odiparpack.transport_planning.repository.RoadSegmentRepository;
import com.odiparpack.transport_planning.repository.TruckRepository;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/planning")
public class PlanningController {

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private PackageOrderRepository packageOrderRepository;

    @GetMapping("/run")
    public String runGLSAlgorithm() {
        try {
            // Obtener los datos necesarios para ejecutar el algoritmo
            List<City> cities = cityRepository.findAll();
            List<RoadSegment> roadSegments = roadSegmentRepository.findAll();
            List<Truck> trucks = truckRepository.findAll();
            List<PackageOrder> packages = packageOrderRepository.findAll();

            // Definir la fecha de inicio de la simulación (1 de marzo de 2023 a las 00:00 horas)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date simulationStartTime = sdf.parse("2023-03-01 00:00:00");

            // Ejecutar el algoritmo GLS con los parámetros requeridos
            glsAlgorithmService.runGLS(cities, roadSegments, trucks, packages, simulationStartTime);
            return "GLS Algorithm executed successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing GLS Algorithm: " + e.getMessage();
        }
    }
}
