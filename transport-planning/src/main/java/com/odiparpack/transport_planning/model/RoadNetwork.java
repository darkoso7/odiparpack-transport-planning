package com.odiparpack.transport_planning.model;

import java.util.*;

public class RoadNetwork {

    private Map<City, List<RoadSegment>> adjacencyList = new HashMap<>();

    public void addRoadSegment(City origin, City destination, RoadSegment roadSegment) {
        List<RoadSegment> originSegments = adjacencyList.computeIfAbsent(origin, k -> new ArrayList<>());
        originSegments.add(roadSegment);
        adjacencyList.computeIfAbsent(destination, k -> new ArrayList<>());
    }

    public List<RoadSegment> getAdjacentSegments(City city) {
        return adjacencyList.getOrDefault(city, new ArrayList<>());
    }

    // Devuelve tanto el costo del camino más corto como la lista de ciudades recorridas
    public ShortestPathResult calculateShortestPathCost(City origin, City destination, Date simulationTime) {
        // Inicializar la cola de prioridad, el mapa de distancias y el almacenamiento de rutas
        PriorityQueue<CityDistancePair> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(CityDistancePair::getCost));
        Map<City, Double> costs = new HashMap<>();
        Map<City, List<City>> routes = new HashMap<>();  // Map para almacenar la ruta de cada ciudad
        Set<City> visited = new HashSet<>();
    
        // Inicializar el costo y la ruta para la ciudad origen
        costs.put(origin, 0.0);
        routes.put(origin, new ArrayList<>(Collections.singletonList(origin)));  // Ruta inicial solo con la ciudad origen
        priorityQueue.add(new CityDistancePair(origin, 0.0));
    
        while (!priorityQueue.isEmpty()) {
            CityDistancePair current = priorityQueue.poll();
            City currentCity = current.getCity();
    
            if (visited.contains(currentCity)) {
                continue;
            }
            visited.add(currentCity);
    
            if (currentCity.equals(destination)) {
                // Devuelve el costo y la ruta completa desde el origen hasta el destino
                return new ShortestPathResult(costs.get(currentCity), routes.get(currentCity));
            }
    
            // Obtener los segmentos adyacentes
            List<RoadSegment> adjacentSegments = getAdjacentSegments(currentCity);
            for (RoadSegment segment : adjacentSegments) {
                if (!segment.isAvailableAt(simulationTime)) {
                    continue;
                }
    
                City neighbor = segment.getDestination();
                double newCost = current.getCost() + segment.getCost();
    
                // Si encontramos un costo menor, actualizamos el costo y la ruta
                if (newCost < costs.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    costs.put(neighbor, newCost);
    
                    // Crear una nueva lista de ciudades para la ruta actualizada
                    List<City> newRoute = new ArrayList<>(routes.get(currentCity));
                    newRoute.add(neighbor);
                    routes.put(neighbor, newRoute);  // Actualizar la ruta para esta ciudad
    
                    priorityQueue.add(new CityDistancePair(neighbor, newCost));
                }
            }
        }
    
        // Si no se encuentra un camino, retornar una distancia infinita o algún valor arbitrario
        return new ShortestPathResult(Double.MAX_VALUE, new ArrayList<>());
    }
    

    private static class CityDistancePair {
        private City city;
        private double cost;

        public CityDistancePair(City city, double cost) {
            this.city = city;
            this.cost = cost;
        }

        public City getCity() {
            return city;
        }

        public double getCost() {
            return cost;
        }
    }
}
