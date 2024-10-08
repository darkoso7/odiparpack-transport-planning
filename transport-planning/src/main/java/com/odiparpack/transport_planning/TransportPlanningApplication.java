package com.odiparpack.transport_planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.odiparpack.transport_planning.model.City;
import com.odiparpack.transport_planning.model.PackageOrder;
import com.odiparpack.transport_planning.model.RoadSegment;
import com.odiparpack.transport_planning.model.Truck;
import com.odiparpack.transport_planning.repository.CityRepository;
import com.odiparpack.transport_planning.repository.PackageOrderRepository;
import com.odiparpack.transport_planning.repository.RoadSegmentRepository;
import com.odiparpack.transport_planning.repository.TruckRepository;
import com.odiparpack.transport_planning.service.DataLoaderService;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.util.List;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@SpringBootApplication
public class TransportPlanningApplication {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private PackageOrderRepository packageOrderRepository;


    public static void main(String[] args) {
        SpringApplication.run(TransportPlanningApplication.class, args);
    }

    @Bean
    public ApplicationRunner initializer() {
        return args -> {
            // Preparación para el monitoreo de memoria
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();  // Cast a la implementación extendida

            List<Long> memoryUsageHistory = new ArrayList<>();
            List<Double> cpuUsageHistory = new ArrayList<>();
            List<Long> timestamps = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            // Cargar los datos de los archivos
            Resource citiesFileResource = resourceLoader.getResource("classpath:data/c.1inf54.24-2.oficinas.v1.0.txt");
            Resource roadSegmentsFileResource = resourceLoader.getResource("classpath:data/road_segments.txt");
            Resource maintenanceFileResource = resourceLoader.getResource("classpath:data/c.1inf54.plan.mant.trim.abr.may.jun.txt");
            Resource breakdownsFileResource = resourceLoader.getResource("classpath:data/averias.txt");

            dataLoaderService.loadCities(citiesFileResource.getFile().getPath());
            dataLoaderService.loadRoadSegments(roadSegmentsFileResource.getFile().getPath());
            dataLoaderService.loadBlockages();
            dataLoaderService.loadTrucksFromData();
            dataLoaderService.loadSalesData();
            dataLoaderService.loadMaintenanceSchedule(maintenanceFileResource.getFile().getPath());
            dataLoaderService.loadScheduledBreakdowns(breakdownsFileResource.getFile().getPath());

            List<City> cities = cityRepository.findAll();
            List<RoadSegment> roadSegments = roadSegmentRepository.findAll();
            List<Truck> trucks = truckRepository.findAll();
            List<PackageOrder> packages = packageOrderRepository.findAll();

            // Monitoreo de memoria y CPU
            Thread monitorThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Memoria usada
                        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
                        memoryUsageHistory.add(heapMemoryUsage.getUsed());
                        
                        // Uso de CPU
                        double cpuLoad = osBean.getCpuLoad();
                        if (cpuLoad >= 0) { // `getSystemLoadAverage` puede devolver -1 en algunos sistemas
                            cpuUsageHistory.add(cpuLoad*100);
                        }

                        timestamps.add(System.currentTimeMillis());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            monitorThread.start();

            Date simulationStartDate = new GregorianCalendar(2023, Calendar.MARCH, 1, 0, 0).getTime();

            try {
                // Ejecutar GLS
                glsAlgorithmService.runGLS(cities, roadSegments, trucks, packages, simulationStartDate);
            } finally {
                monitorThread.interrupt();

                // Crear gráficas de uso de memoria

                createMemoryUsageChart(memoryUsageHistory, timestamps);
                long endTime = System.currentTimeMillis();
                double executionTimeSeconds = (endTime - startTime) / 1000.0;
                System.out.printf("Tiempo de ejecución (segundos): %.2f%n", executionTimeSeconds);
    
                // Crear gráficas de uso de CPU

                createCpuUsageChart(cpuUsageHistory, timestamps);
                double averageCpuUsage = cpuUsageHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                System.out.printf("Uso promedio de CPU: %.2f%%%n", averageCpuUsage * 100);
            }
        };
    }

    private void createMemoryUsageChart(List<Long> memoryUsage, List<Long> timestamps) {
        XYSeries series = new XYSeries("Memory Usage");

        for (int i = 0; i < memoryUsage.size(); i++) {
            // Convertir timestamps a segundos desde el inicio
            long timeInSeconds = (timestamps.get(i) - timestamps.get(0)) / 1000;
            series.add(timeInSeconds, memoryUsage.get(i) / (1024 * 1024)); // Convertir a MB
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Memory Usage Over Time",
                "Time (s)",
                "Memory (MB)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Guardar la gráfica como imagen
        try {
            ChartUtils.saveChartAsPNG(new File("memory_usage_chart.png"), chart, 800, 600);
            System.out.println("Gráfica guardada como memory_usage_chart.png");
        } catch (IOException e) {
            System.err.println("Error al guardar la gráfica: " + e.getMessage());
        }

        
    }

    private void createCpuUsageChart(List<Double> cpuUsage, List<Long> timestamps) {
        XYSeries series = new XYSeries("CPU Usage");

        for (int i = 0; i < cpuUsage.size(); i++) {
            // Convertir timestamps a segundos desde el inicio
            long timeInSeconds = (timestamps.get(i) - timestamps.get(0)) / 1000;
            series.add(timeInSeconds, cpuUsage.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "CPU Usage Over Time",
                "Time (s)",
                "CPU Load (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Guardar la gráfica como imagen
        try {
            ChartUtils.saveChartAsPNG(new File("cpu_usage_chart.png"), chart, 800, 600);
            System.out.println("Gráfica de CPU guardada como cpu_usage_chart.png");
        } catch (IOException e) {
            System.err.println("Error al guardar la gráfica de CPU: " + e.getMessage());
        }
    }
}
