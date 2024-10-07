package com.odiparpack.transport_planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Date;

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
            // Preparación para el monitoreo de memoria
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            List<Long> memoryUsageHistory = new ArrayList<>();
            List<Long> timestamps = new ArrayList<>();

            // Thread para medir la memoria cada segundo mientras se ejecuta el código
            Thread memoryMonitor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
                    memoryUsageHistory.add(heapMemoryUsage.getUsed());
                    timestamps.add(System.currentTimeMillis());
                    try {
                        Thread.sleep(1000); // medir cada segundo
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

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

            // Invocar a los repositorios
            
            // Construir la red de carreteras

            // Iniciar el monitoreo de memoria
            memoryMonitor.start();

            try {
                // Correr el algoritmo GLS
                glsAlgorithmService.runGLS(new Date());
            } finally {
                // Parar el monitoreo de memoria al finalizar la ejecución
                memoryMonitor.interrupt();
            }

            // Graficar el uso de memoria
            createMemoryUsageChart(memoryUsageHistory, timestamps);
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
}
