package com.odiparpack.transport_planning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.odiparpack.transport_planning.repository.*;
import com.odiparpack.transport_planning.model.*;
import java.util.*;
import java.util.stream.Collectors;

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

    public void runGLS() {
        // Initialize data
        List<City> cities = cityRepository.findAll();
        List<RoadSegment> roadSegments = roadSegmentRepository.findAll();
        List<Truck> trucks = truckRepository.findAll();
        List<PackageOrder> packages = packageOrderRepository.findAll();
        penalties = new HashMap<>();
        lambda = calculateLambda(roadSegments);

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
        }

        // Save the best solution
        for (TransportationPlan plan : bestSolution) {
            transportationPlanRepository.save(plan);
        }

        // Print final best solution and cost
        System.out.println("Final best solution cost: " + bestCost);
        printSolution(bestSolution);
    }

    private void printSolution(List<TransportationPlan> solution) {
        for (TransportationPlan plan : solution) {
            System.out.println("Truck: " + plan.getTruck().getCode());
            System.out.println("Route:");
            for (City city : plan.getRoute()) {
                System.out.println(" - " + city.getProvince() + " (" + city.getUbigeo() + ")");
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

    private List<TransportationPlan> generateInitialSolution(List<Truck> trucks, List<PackageOrder> packages) {
        List<TransportationPlan> plans = new ArrayList<>();

        // Sort packages by earliest delivery deadline
        packages.sort(Comparator.comparing(PackageOrder::getDeliveryDeadline));

        // Assign packages to trucks
        for (Truck truck : trucks) {
            if (!truck.isAvailable()) continue;

            List<PackageOrder> assignedPackages = new ArrayList<>();
            int remainingCapacity = truck.getCapacity();

            Iterator<PackageOrder> pkgIterator = packages.iterator();
            while (pkgIterator.hasNext()) {
                PackageOrder pkg = pkgIterator.next();
                int pkgQuantity = pkg.getQuantity();

                if (remainingCapacity >= pkgQuantity) {
                    // Assign full package
                    assignedPackages.add(pkg);
                    remainingCapacity -= pkgQuantity;
                    pkgIterator.remove(); // Remove from the list
                } else if (remainingCapacity > 0) {
                    // Assign partial package
                    PackageOrder partialPkg = new PackageOrder();
                    partialPkg.setOrderId(pkg.getOrderId());
                    partialPkg.setQuantity(remainingCapacity);
                    partialPkg.setDestination(pkg.getDestination());
                    partialPkg.setOrderDate(pkg.getOrderDate());
                    partialPkg.setDeliveryDeadline(pkg.getDeliveryDeadline());

                    assignedPackages.add(partialPkg);

                    // Update the original package quantity
                    pkg.setQuantity(pkgQuantity - remainingCapacity);
                    remainingCapacity = 0;
                }

                if (remainingCapacity == 0) break;
            }

            if (!assignedPackages.isEmpty()) {
                // Plan route
                List<City> route = planRoute(truck.getCurrentLocation(), assignedPackages);

                // Create TransportationPlan for this truck
                TransportationPlan plan = new TransportationPlan();
                plan.setTruck(truck);
                plan.setRoute(route);
                plan.setDeliveries(assignedPackages);
                plans.add(plan);
            }
        }

        return plans;
    }

    private List<City> planRoute(City startLocation, List<PackageOrder> packages) {
        // Simple nearest neighbor algorithm to plan the route
        List<City> route = new ArrayList<>();
        route.add(startLocation);
        City currentCity = startLocation;
        List<PackageOrder> remainingPackages = new ArrayList<>(packages);

        while (!remainingPackages.isEmpty()) {
            PackageOrder nextPackage = findNearestPackage(currentCity, remainingPackages);
            City nextCity = nextPackage.getDestination();
            route.add(nextCity);
            currentCity = nextCity;
            remainingPackages.remove(nextPackage);
        }

        // Return to start location if needed
        if (!currentCity.equals(startLocation)) {
            route.add(startLocation);
        }

        return route;
    }

    private PackageOrder findNearestPackage(City currentCity, List<PackageOrder> packages) {
        PackageOrder nearestPackage = null;
        double minDistance = Double.MAX_VALUE;

        for (PackageOrder pkg : packages) {
            City destination = pkg.getDestination();
            double distance = calculateDistance(currentCity, destination);
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

        // Example: Try swapping packages between trucks to reduce cost
        for (int i = 0; i < newSolution.size(); i++) {
            for (int j = i + 1; j < newSolution.size(); j++) {
                TransportationPlan tp1 = newSolution.get(i);
                TransportationPlan tp2 = newSolution.get(j);

                // Swap packages between tp1 and tp2
                List<PackageOrder> packages1 = new ArrayList<>(tp1.getDeliveries());
                List<PackageOrder> packages2 = new ArrayList<>(tp2.getDeliveries());

                for (PackageOrder pkg1 : packages1) {
                    for (PackageOrder pkg2 : packages2) {
                        // Swap if capacity constraints are satisfied
                        if (canSwapPackages(tp1, pkg1, tp2, pkg2)) {
                            swapPackages(tp1, pkg1, tp2, pkg2);
                            // Recalculate routes
                            tp1.setRoute(planRoute(tp1.getTruck().getCurrentLocation(), tp1.getDeliveries()));
                            tp2.setRoute(planRoute(tp2.getTruck().getCurrentLocation(), tp2.getDeliveries()));
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

        // Calculate new weights after swap
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
        // Calculate total cost including penalties
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
                    // Penalize late deliveries heavily
                    cost += 10000 * (estimatedDeliveryTime.getTime() - pkg.getDeliveryDeadline().getTime()) / (1000 * 60 * 60);
                }
            }
        }

        return cost;
    }

    private Date estimateDeliveryTime(TransportationPlan tp, PackageOrder pkg) {
        // Estimate the delivery time based on the route and speed limits
        Date startTime = tp.getTruck().getAvailableFrom();
        Date currentTime = new Date(startTime.getTime());

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
}
