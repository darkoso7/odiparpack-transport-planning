package com.odiparpack.transport_planning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.odiparpack.transport_planning.repository.*;
import com.odiparpack.transport_planning.model.*;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

@Service
public class GLSAlgorithmService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private PackageOrderRepository packageOrderRepository;

    @Autowired
    private TransportationPlanRepository transportationPlanRepository;

    private Map<RoadSegment, Integer> penalties;
    private double lambda;
    private RoadNetwork roadNetwork;

    public void runGLS(Date simulationStartTime) {
    public void runGLS(Date startDate, Date endDate) {
        // Initialize data
        List<City> cities = cityRepository.findAll();
        List<RoadSegment> roadSegments = roadSegmentRepository.findAll();
        List<Truck> trucks = truckRepository.findAll();
        List<PackageOrder> packages = packageOrderRepository.findAll();
        
        penalties = new HashMap<>();
        lambda = calculateLambda(roadSegments);
        roadNetwork = new RoadNetwork();

        // Construir la red de carreteras
        buildRoadNetwork(cities, roadSegments);

        // Solución inicial
        List<TransportationPlan> currentSolution = generateInitialSolution(trucks, packages, simulationStartTime);
        List<TransportationPlan> bestSolution = new ArrayList<>(currentSolution);
        double bestTime = calculateTotalDeliveryTime(bestSolution, simulationStartTime);
    
        // Parámetros de Simulated Annealing
        double temperature = 1000; // Temperatura inicial
        double coolingRate = 0.995; // Tasa de enfriamiento
        int maxIterations = 1000;
    
        // Initialize penalties
        for (RoadSegment rs : roadSegments) {
            penalties.put(rs, 0);
        }
    
        // Generate initial solution
        List<TransportationPlan> currentSolution = generateInitialSolution(trucks, packages);
        List<TransportationPlan> bestSolution = currentSolution;
        double bestCost = calculateObjectiveFunction(bestSolution);
    
        // GLS iterations
        int iteration = 0;
    
        // GLS con Simulated Annealing
        while (iteration < maxIterations && temperature > 1) {
            // Búsqueda local
            List<TransportationPlan> newSolution = localSearch(currentSolution, simulationStartTime);
            double newTime = calculateTotalDeliveryTime(newSolution, simulationStartTime);
    
            // Comparar con la mejor solución
            if (acceptSolution(newTime, bestTime, temperature)) {
                currentSolution = newSolution;
                if (newTime < bestTime) {
                    bestSolution = new ArrayList<>(newSolution);
                    bestTime = newTime;
                }
            }
    
            // Reducir la temperatura
            temperature *= coolingRate;
        int maxIterations = 100;
    
        while (iteration < maxIterations) {
            // Local Search
            List<TransportationPlan> localOptimum = localSearch(currentSolution);
    
            // Update best solution
            double localCost = calculateObjectiveFunction(localOptimum);
            if (localCost < bestCost) {
                bestSolution = localOptimum;
                bestCost = localCost;
            }
    
            // Update penalties
            updatePenalties(localOptimum);
    
            // Move to next solution
            currentSolution = localOptimum;
            iteration++;
    
            // Actualizar penalizaciones (GLS)
            updatePenalties(newSolution);
        }
    
        // Guardar la mejor solución encontrada
    
        // Save the best solution
        for (TransportationPlan plan : bestSolution) {
            transportationPlanRepository.save(plan);
        }
    
        // Mostrar la solución final
        System.out.println("Best solution total time: " + bestTime);
        printSolution(bestSolution);
    }
    
        // Print final best solution and cost
        System.out.println("Final best solution cost: " + bestCost);
        printSolutionWithTime(bestSolution, startDate, endDate);
    }    

    // Construir la red de carreteras
    private void buildRoadNetwork(List<City> cities, List<RoadSegment> roadSegments) {
        for (RoadSegment segment : roadSegments) {
            roadNetwork.addRoadSegment(segment.getOrigin(), segment.getDestination(), segment);
    private void printSolutionWithTime(List<TransportationPlan> solution, Date startDate, Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (TransportationPlan plan : solution) {
            System.out.println("Truck: " + plan.getTruck().getCode());
            System.out.println("Route:");
            Date currentTime = plan.getTruck().getAvailableFrom();
            for (int i = 0; i < plan.getRoute().size() - 1; i++) {
                City origin = plan.getRoute().get(i);
                City destination = plan.getRoute().get(i + 1);
                RoadSegment roadSegment = roadSegmentRepository.findByOriginAndDestination(origin, destination);

                // Calculate travel time
                double distance = roadSegment.getDistance();
                double speedLimit = roadSegment.getSpeedLimit();
                double travelTimeHours = distance / speedLimit;
                long travelTimeMs = (long) (travelTimeHours * 3600 * 1000);

                // Set the end time for the current segment
                Date arrivalTime = new Date(currentTime.getTime() + travelTimeMs);

                // Print times for each segment
                System.out.println(" - From " + origin.getProvince() + " (" + origin.getUbigeo() + ") to " + destination.getProvince() + " (" + destination.getUbigeo() + ")");
                System.out.println("   Start time: " + sdf.format(currentTime) + ", Arrival time: " + sdf.format(arrivalTime));

                // Update current time for next segment
                currentTime = arrivalTime;
            }

            System.out.println("Deliveries:");
            for (PackageOrder pkg : plan.getDeliveries()) {
                System.out.println(" - Package " + pkg.getOrderId() + " to " + pkg.getDestination().getProvince() + " (" + pkg.getDestination().getUbigeo() + ")");
            }
            System.out.println();
        }
    }


    private double calculateLambda(List<RoadSegment> roadSegments) {
        double totalCost = 0;
        for (RoadSegment rs : roadSegments) {
            totalCost += rs.getCost();
        }
        return 0.1 * (totalCost / roadSegments.size());
    }

    private boolean acceptSolution(double newTime, double currentTime, double temperature) {
        if (newTime < currentTime) {
            return true; // Aceptar una mejor solución
        }
        // Aceptar una solución peor con cierta probabilidad
        return Math.random() < Math.exp((currentTime - newTime) / temperature);
    }

    private void updatePenalties(List<TransportationPlan> solution) {
        List<RoadSegment> overusedSegments = identifyOverusedSegments(solution);
        
        for (RoadSegment rs : overusedSegments) {
            int currentPenalty = penalties.getOrDefault(rs, 0);
            penalties.put(rs, currentPenalty + 1); // Aumentar la penalización en los segmentos sobreutilizados
        }
    }

    private List<RoadSegment> identifyOverusedSegments(List<TransportationPlan> solution) {
        Map<RoadSegment, Integer> segmentUsage = new HashMap<>();

        for (TransportationPlan tp : solution) {
            List<City> route = tp.getRoute();
            for (int i = 0; i < route.size() - 1; i++) {
                City origin = route.get(i);
                City destination = route.get(i + 1);
                RoadSegment rs = roadSegmentRepository.findByOriginAndDestination(origin, destination);
                if (rs != null) {
                    segmentUsage.put(rs, segmentUsage.getOrDefault(rs, 0) + 1);
                }
            }
        }

        int usageThreshold = 1; // Ajusta este umbral si es necesario
        return segmentUsage.entrySet().stream()
                .filter(entry -> entry.getValue() > usageThreshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Método que genera la solución inicial, considerando bloqueos, mantenimientos y averías
    private List<TransportationPlan> generateInitialSolution(List<Truck> trucks, List<PackageOrder> packages, Date simulationStartTime) {
        List<TransportationPlan> plans = new ArrayList<>();

        packages.sort(Comparator.comparing(PackageOrder::getDeliveryDeadline));

        for (Truck truck : trucks) {
            // Verificar si el camión está disponible (sin mantenimiento ni averías)
            if (!truck.isAvailable() || !truck.isOperational(simulationStartTime)) continue;

            List<PackageOrder> assignedPackages = new ArrayList<>();
            int remainingCapacity = truck.getCapacity();

             Iterator<PackageOrder> pkgIterator = packages.iterator();
            while (pkgIterator.hasNext()) {
                PackageOrder pkg = pkgIterator.next();
                int pkgQuantity = pkg.getQuantity();

                // Asignar paquetes según la capacidad del camión
                if (remainingCapacity >= pkgQuantity) {
                    assignedPackages.add(pkg);
                    remainingCapacity -= pkgQuantity;
                    pkgIterator.remove();
                } else if (remainingCapacity > 0) {
                    // Asignación parcial
                    PackageOrder partialPkg = new PackageOrder();
                    partialPkg.setOrderId(pkg.getOrderId());
                    partialPkg.setQuantity(remainingCapacity);
                    partialPkg.setDestination(pkg.getDestination());
                    partialPkg.setOrderDate(pkg.getOrderDate());
                    partialPkg.setDeliveryDeadline(pkg.getDeliveryDeadline());

                    assignedPackages.add(partialPkg);
                    pkg.setQuantity(pkgQuantity - remainingCapacity);
                    remainingCapacity = 0;
                }

                if (remainingCapacity == 0) break;
            }

            if (!assignedPackages.isEmpty()) {
                List<City> route = planRouteUsingNetwork(truck.getCurrentLocation(), assignedPackages, simulationStartTime);

                TransportationPlan plan = new TransportationPlan();
                plan.setTruck(truck);
                plan.setRoute(route);
                plan.setDeliveries(assignedPackages);
                plans.add(plan);
            }
        }

        return plans;
    }

    // Planificar la ruta utilizando la red de carreteras y considerando bloqueos
    private List<City> planRouteUsingNetwork(City startLocation, List<PackageOrder> packages, Date simulationStartTime) {
        List<City> route = new ArrayList<>();
        route.add(startLocation);
        City currentCity = startLocation;
        List<PackageOrder> remainingPackages = new ArrayList<>(packages);
    
        while (!remainingPackages.isEmpty()) {
            PackageOrder nextPackage = findNearestPackageUsingNetwork(currentCity, remainingPackages, simulationStartTime);
            if (nextPackage == null) {
                break;
            }
    
            City nextCity = nextPackage.getDestination();
            ShortestPathResult pathResult = calculateShortestPathDistance(currentCity, nextCity, simulationStartTime);
            
            // Asegurarse de agregar todas las ciudades intermedias de la ruta
            route.addAll(pathResult.getPath().subList(1, pathResult.getPath().size()));
            currentCity = nextCity;
            remainingPackages.remove(nextPackage);
        }
    
        // Retornar a la ubicación inicial si es necesario
        if (!currentCity.equals(startLocation)) {
            ShortestPathResult returnPath = calculateShortestPathDistance(currentCity, startLocation, simulationStartTime);
            route.addAll(returnPath.getPath().subList(1, returnPath.getPath().size()));
        }
    
        return route;
    }
    
    
    // Encontrar el paquete más cercano utilizando la red de carreteras, considerando bloqueos
    private PackageOrder findNearestPackageUsingNetwork(City currentCity, List<PackageOrder> packages, Date simulationStartTime) {
        PackageOrder nearestPackage = null;
        double minDistance = Double.MAX_VALUE;
    
        for (PackageOrder pkg : packages) {
            City destination = pkg.getDestination();
            double distance = calculateShortestPathDistance(currentCity, destination, simulationStartTime).getCost();
    
            // Validación adicional: si no se encuentra un camino, omitir el paquete
            if (distance == Double.MAX_VALUE) {
                continue;
            }
    
            if (distance < minDistance) {
                minDistance = distance;
                nearestPackage = pkg;
            }
        }
    
        return nearestPackage;
    }

    private double calculateDistance(City origin, City destination) {
        // Use Haversine formula or any method to calculate distance
        double lat1 = origin.getLatitude();
        double lon1 = origin.getLongitude();
        double lat2 = destination.getLatitude();
        double lon2 = destination.getLongitude();

        // Haversine formula
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 + Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }

    private List<TransportationPlan> localSearch(List<TransportationPlan> solution) {
        // Implement local search strategies
        // For example, swap deliveries between trucks, 2-opt on routes, etc.

        List<TransportationPlan> newSolution = copyTransportationPlans(solution);

        // Ejemplo: Intentar intercambiar paquetes entre camiones
        for (int i = 0; i < newSolution.size(); i++) {
            for (int j = i + 1; j < newSolution.size(); j++) {
                TransportationPlan tp1 = newSolution.get(i);
                TransportationPlan tp2 = newSolution.get(j);

                // Intercambiar paquetes si mejora el tiempo total
                List<PackageOrder> packages1 = new ArrayList<>(tp1.getDeliveries());
                List<PackageOrder> packages2 = new ArrayList<>(tp2.getDeliveries());

                for (PackageOrder pkg1 : packages1) {
                    for (PackageOrder pkg2 : packages2) {
                        if (canSwapPackages(tp1, pkg1, tp2, pkg2)) {
                            swapPackages(tp1, pkg1, tp2, pkg2);
                            // Recalcular las rutas y el tiempo de entrega utilizando roadNetwork
                            tp1.setRoute(planRouteUsingNetwork(tp1.getTruck().getCurrentLocation(), tp1.getDeliveries(), simulationStartTime));
                            tp2.setRoute(planRouteUsingNetwork(tp2.getTruck().getCurrentLocation(), tp2.getDeliveries(), simulationStartTime));
                            break;
                        }
                    }
                }
            }
        }

        return newSolution;
    }

    private boolean canSwapPackages(TransportationPlan tp1, PackageOrder pkg1, TransportationPlan tp2, PackageOrder pkg2) {
        int capacity1 = tp1.getTruck().getCapacity();
        int capacity2 = tp2.getTruck().getCapacity();

        int totalWeightTp1 = tp1.getDeliveries().stream().mapToInt(PackageOrder::getQuantity).sum();
        int totalWeightTp2 = tp2.getDeliveries().stream().mapToInt(PackageOrder::getQuantity).sum();

        int newWeightTp1 = totalWeightTp1 - pkg1.getQuantity() + pkg2.getQuantity();
        int newWeightTp2 = totalWeightTp2 - pkg2.getQuantity() + pkg1.getQuantity();

        return newWeightTp1 <= capacity1 && newWeightTp2 <= capacity2;
    }

    private void swapPackages(TransportationPlan tp1, PackageOrder pkg1, TransportationPlan tp2, PackageOrder pkg2) {
        tp1.getDeliveries().remove(pkg1);
        tp2.getDeliveries().remove(pkg2);

        tp1.getDeliveries().add(pkg2);
        tp2.getDeliveries().add(pkg1);
    }

    private void updatePenalties(List<TransportationPlan> solution) {
        // Identify features to penalize and update penalties map
        // For example, penalize overused road segments
        List<RoadSegment> overusedSegments = identifyOverusedSegments(solution);

        for (RoadSegment rs : overusedSegments) {
            int currentPenalty = penalties.getOrDefault(rs, 0);
            penalties.put(rs, currentPenalty + 1);
        }
    }

    private List<RoadSegment> identifyOverusedSegments(List<TransportationPlan> solution) {
        Map<RoadSegment, Integer> segmentUsage = new HashMap<>();

        for (TransportationPlan tp : solution) {
            List<City> route = tp.getRoute();
            for (int i = 0; i < route.size() - 1; i++) {
                City origin = route.get(i);
                City destination = route.get(i + 1);
                RoadSegment rs = roadSegmentRepository.findByOriginAndDestination(origin, destination);
                if (rs != null) {
                    segmentUsage.put(rs, segmentUsage.getOrDefault(rs, 0) + 1);
                }
            }
        }

        // Identify segments used more than a threshold
        int usageThreshold = 1; // This threshold can be adjusted
        List<RoadSegment> overusedSegments = segmentUsage.entrySet().stream()
            .filter(entry -> entry.getValue() > usageThreshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return overusedSegments;
    }

    private double calculateObjectiveFunction(List<TransportationPlan> solution) {
        double cost = 0.0;
    
        for (TransportationPlan tp : solution) {
            List<City> route = tp.getRoute();
            for (int i = 0; i < route.size() - 1; i++) {
                City origin = route.get(i);
                City destination = route.get(i + 1);
                RoadSegment rs = roadSegmentRepository.findByOriginAndDestination(origin, destination);
                if (rs != null) {
                    int penalty = penalties.getOrDefault(rs, 0);
                    cost += rs.getCost() + (lambda * penalty);
                }
            }
    
            // Add penalties for late deliveries
            for (PackageOrder pkg : tp.getDeliveries()) {
                Date estimatedDeliveryTime = estimateDeliveryTime(tp, pkg);
                if (estimatedDeliveryTime.after(pkg.getDeliveryDeadline())) {
                    cost += 10000 * (estimatedDeliveryTime.getTime() - pkg.getDeliveryDeadline().getTime()) / (1000 * 60 * 60);
                }
            }
        }
    
        return cost;
    }
    

    private Date estimateDeliveryTime(TransportationPlan tp, PackageOrder pkg) {
        Date currentTime = new Date(tp.getTruck().getAvailableFrom().getTime());
    
        List<City> route = tp.getRoute();
        for (int i = 0; i < route.size() - 1; i++) {
            City origin = route.get(i);
            City destination = route.get(i + 1);
            RoadSegment rs = roadSegmentRepository.findByOriginAndDestination(origin, destination);
            if (rs != null) {
                double distance = rs.getDistance();
                double speedLimit = rs.getSpeedLimit();
                double travelTimeHours = distance / speedLimit;
    
                // Convert travel time to milliseconds and add to current time
                long travelTimeMs = (long) (travelTimeHours * 3600 * 1000);
                currentTime = new Date(currentTime.getTime() + travelTimeMs);
    
                // Check if destination is the package destination
                if (destination.equals(pkg.getDestination())) {
                    break;
                }
            }
        }
    
        return currentTime;
    }
    

    private List<TransportationPlan> copyTransportationPlans(List<TransportationPlan> originalPlans) {
        List<TransportationPlan> copiedPlans = new ArrayList<>();
        for (TransportationPlan tp : originalPlans) {
            TransportationPlan copy = new TransportationPlan();
            copy.setTruck(tp.getTruck());
            copy.setRoute(new ArrayList<>(tp.getRoute()));
            copy.setDeliveries(new ArrayList<>(tp.getDeliveries()));
            copiedPlans.add(copy);
        }
        return copiedPlans;
    }

    private void printSolution(List<TransportationPlan> solution) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date initialStartDate = new Date(); // Hora de inicio para la simulación
    
        for (TransportationPlan plan : solution) {
            System.out.println("Camión: " + plan.getTruck().getCode());
            Date currentTime = new Date(initialStartDate.getTime());
            System.out.println("Fecha de inicio: " + sdf.format(currentTime));
    
            System.out.println("Ruta:");
            List<City> route = plan.getRoute();
            int remainingCapacity = plan.getTruck().getCapacity();
            
            // Crear un mapa para almacenar la fecha de entrega de cada ciudad en la ruta
            Map<City, Date> deliveryTimes = new HashMap<>();
    
            // Iterar a través de la ruta y calcular los tiempos de llegada
            for (int i = 0; i < route.size() - 1; i++) {
                City origin = route.get(i);
                City destination = route.get(i + 1);
                RoadSegment rs = roadSegmentRepository.findByOriginAndDestination(origin, destination);
    
                if (rs != null) {
                    System.out.println(" - Saliendo de " + origin.getProvince() + " (" + origin.getUbigeo() + ") a las " + sdf.format(currentTime));
    
                    // Calcular el tiempo de viaje
                    double travelTimeHours = rs.getDistance() / rs.getSpeedLimit();
                    long travelTimeMs = (long) (travelTimeHours * 3600 * 1000);
                    currentTime = new Date(currentTime.getTime() + travelTimeMs);
    
                    // Registrar el tiempo de llegada a esta ciudad en el mapa
                    deliveryTimes.put(destination, currentTime);
    
                    System.out.println(" - Llegando a " + destination.getProvince() + " (" + destination.getUbigeo() + ") a las " + sdf.format(currentTime));
                    System.out.println("   Costo: " + rs.getCost() + " | Tiempo: " + travelTimeHours + " horas | Distancia: " + rs.getDistance() + " km");
                }
            }
    
            System.out.println("Entregas:");
            for (PackageOrder pkg : plan.getDeliveries()) {
                // Verificar si la ciudad de entrega del paquete está en la ruta
                Date deliveryTime = deliveryTimes.get(pkg.getDestination());
    
                if (deliveryTime != null) {
                    // Mostrar la fecha exacta de entrega en base a la llegada del camión
                    System.out.println(" - Paquete " + pkg.getOrderId() + " (Cantidad: " + pkg.getQuantity() + ") a " + pkg.getDestination().getProvince() + " (" + pkg.getDestination().getUbigeo() + ")");
                    System.out.println("   Fecha de entrega: " + sdf.format(deliveryTime) + " (Capacidad restante del camión: " + remainingCapacity + ")");
                    remainingCapacity -= pkg.getQuantity();  // Actualizar la capacidad después de la entrega
                } else {
                    System.out.println(" - Paquete " + pkg.getOrderId() + " no pudo ser entregado en la ruta");
                }
            }
            System.out.println();
        }
    }
    
    
    
}
