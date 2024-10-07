package com.odiparpack.transport_planning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.odiparpack.transport_planning.repository.*;
import com.odiparpack.transport_planning.model.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

@Service
public class DataLoaderService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private PackageOrderRepository packageOrderRepository;

    private static final Logger logger = LoggerFactory.getLogger(DataLoaderService.class);

    // Updated format to handle both date and time
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

    /**
     * Loads cities from a file and saves them to the database.
     * @param filePath Path to the cities data file.
     */
    public void loadCities(String filePath) {
        logger.info("Starting to load cities from file: {}", filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] tokens = line.split(",");
                if (tokens.length < 7) {
                    logger.warn("Invalid line at {}: {}", lineNumber, line);
                    continue;
                }
                City city = new City();
                city.setUbigeo(tokens[0]);
                city.setDepartment(tokens[1]);
                city.setProvince(tokens[2]);
                city.setLatitude(Double.parseDouble(tokens[3]));
                city.setLongitude(Double.parseDouble(tokens[4]));
                city.setRegion(tokens[5]);
                city.setWarehouseCapacity(Integer.parseInt(tokens[6]));
                cityRepository.save(city);
                logger.debug("Saved city: {}", city);
            }
            logger.info("Finished loading cities from file: {}", filePath);
        } catch (IOException e) {
            logger.error("Error reading cities file: {}", filePath, e);
        } catch (Exception e) {
            logger.error("Error processing cities file: {}", filePath, e);
        }
    }
    

    /**
     * Loads road segments from a file and saves them to the database.
     * @param filePath Path to the road segments data file.
     */
    public void loadRoadSegments(String filePath) {
        logger.info("Starting to load road segments from file: {}", filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                // Assuming format: UG-Ori => UG-Des
                String[] tokens = line.split("=>");
                if (tokens.length != 2) {
                    logger.warn("Invalid line at {}: {}", lineNumber, line);
                    continue;
                }
                String originUbigeo = tokens[0].trim();
                String destinationUbigeo = tokens[1].trim();
    
                City origin = cityRepository.findById(originUbigeo).orElse(null);
                City destination = cityRepository.findById(destinationUbigeo).orElse(null);
    
                if (origin != null && destination != null) {
                    RoadSegment rs = new RoadSegment();
                    rs.setOrigin(origin);
                    rs.setDestination(destination);

                    // Calculate distance and speed limit
                    double distance = calculateDistance(origin, destination);
                    double speedLimit = getSpeedLimit(origin.getRegion(), destination.getRegion());

                    rs.setDistance(distance);
                    rs.setSpeedLimit(speedLimit);

                    // Calculate initial cost as time (distance / speedLimit)
                    double timeCost = distance / speedLimit;
                    rs.setCost((int) Math.round(timeCost)); // Rounding to the nearest integer

                    roadSegmentRepository.save(rs);
                    logger.debug("Saved road segment: {} => {} with cost: {}", originUbigeo, destinationUbigeo, rs.getCost());
                } else {
                    if (origin == null) {
                        logger.warn("Origin city not found for UBIGEO: {} at line {}", originUbigeo, lineNumber);
                    }
                    if (destination == null) {
                        logger.warn("Destination city not found for UBIGEO: {} at line {}", destinationUbigeo, lineNumber);
                    }
                }
            }
            logger.info("Finished loading road segments from file: {}", filePath);
        } catch (IOException e) {
            logger.error("Error reading road segments file: {}", filePath, e);
        }
    }
    

    /**
     * Calculates the distance between two cities using the Haversine formula.
     * @param origin The origin city.
     * @param destination The destination city.
     * @return The distance in kilometers.
     */
    private double calculateDistance(City origin, City destination) {
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
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }

    /**
     * Determines the speed limit between two regions.
     * @param region1 The region of the origin city.
     * @param region2 The region of the destination city.
     * @return The speed limit in km/h.
     */
    private double getSpeedLimit(String region1, String region2) {
        // Define speed limits based on regions
        if (region1.equalsIgnoreCase("COSTA") && region2.equalsIgnoreCase("COSTA"))
            return 70.0;
        if ((region1.equalsIgnoreCase("COSTA") && region2.equalsIgnoreCase("SIERRA")) ||
            (region1.equalsIgnoreCase("SIERRA") && region2.equalsIgnoreCase("COSTA")))
            return 50.0;
        if (region1.equalsIgnoreCase("SIERRA") && region2.equalsIgnoreCase("SIERRA"))
            return 60.0;
        if ((region1.equalsIgnoreCase("SIERRA") && region2.equalsIgnoreCase("SELVA")) ||
            (region1.equalsIgnoreCase("SELVA") && region2.equalsIgnoreCase("SIERRA")))
            return 55.0;
        if (region1.equalsIgnoreCase("SELVA") && region2.equalsIgnoreCase("SELVA"))
            return 65.0;
        return 50.0; // Default speed limit
    }

    /**
     * Loads trucks from a file and saves them to the database.
     * @param filePath Path to the trucks data file.
     */
    public void loadTrucksFromData() {
        // Define the data
        List<TruckData> truckDataList = new ArrayList<>();

        // Lima Trucks
        truckDataList.add(new TruckData("A", 90, "Lima", Arrays.asList("A001", "A002", "A003", "A004")));
        truckDataList.add(new TruckData("B", 45, "Lima", Arrays.asList("B001", "B002", "B003", "B004", "B005", "B006", "B007")));
        truckDataList.add(new TruckData("C", 30, "Lima", Arrays.asList("C001", "C002", "C003", "C004", "C005", "C006", "C007", "C008", "C009", "C010")));

        // Trujillo Trucks
        truckDataList.add(new TruckData("A", 90, "Trujillo", Arrays.asList("A005")));
        truckDataList.add(new TruckData("B", 45, "Trujillo", Arrays.asList("B008", "B009", "B010")));
        truckDataList.add(new TruckData("C", 30, "Trujillo", Arrays.asList("C011", "C012", "C013", "C014", "C015", "C016")));

        // Arequipa Trucks
        truckDataList.add(new TruckData("A", 90, "Arequipa", Arrays.asList("A006")));
        truckDataList.add(new TruckData("B", 45, "Arequipa", Arrays.asList("B011", "B012", "B013", "B014", "B015")));
        truckDataList.add(new TruckData("C", 30, "Arequipa", Arrays.asList("C017", "C018", "C019", "C020", "C021", "C022", "C023", "C024")));

        // Load trucks into the database
        for (TruckData td : truckDataList) {
            loadTrucks(td);
        }
    }

    private void loadTrucks(TruckData truckData) {
        // Find the city by province name (case-insensitive)
        City location = cityRepository.findByProvinceIgnoreCase(truckData.location);
    
        if (location == null) {
            logger.warn("City not found for location: {}", truckData.location);
            return;
        }
    
        for (String code : truckData.codes) {
            Truck truck = new Truck();
            truck.setCode(code);
            truck.setType(truckData.type);
            truck.setCapacity(truckData.capacity);
            truck.setCurrentLocation(location);
            truck.setAvailable(true);
            truck.setAvailableFrom(new Date());
            truckRepository.save(truck);
            logger.debug("Saved truck: {} of type {} at location {}", code, truckData.type, truckData.location);
        }
    }

    private static class TruckData {
        String type;
        int capacity;
        String location; // City name
        List<String> codes;

        public TruckData(String type, int capacity, String location, List<String> codes) {
            this.type = type;
            this.capacity = capacity;
            this.location = location;
            this.codes = codes;
        }
    }

    /**
     * Calculates the delivery deadline based on the region.
     * @param region Destination region.
     * @param orderDate Date the order was placed.
     * @return Delivery deadline Date.
     */
    private Date calculateDeadline(String region, Date orderDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(orderDate);

        switch (region.toUpperCase()) {
            case "COSTA":
                cal.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "SIERRA":
                cal.add(Calendar.DAY_OF_MONTH, 2);
                break;
            case "SELVA":
                cal.add(Calendar.DAY_OF_MONTH, 3);
                break;
            default:
                cal.add(Calendar.DAY_OF_MONTH, 2); // Default to 2 days
                break;
        }
        return cal.getTime();
    }

    public void loadSalesData() {
        try {
            // Adjust the path to your data directory
            String dataDir = "classpath:data/";
            Resource[] resources = getResources(dataDir + "c.1inf54.ventas*.txt");

            for (Resource resource : resources) {
                System.out.println("Loading file: " + resource.getFilename());
                loadSalesDataFromFile(resource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSalesDataFromFile(Resource resource) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                parseAndSavePackageOrder(line, resource.getFilename());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseAndSavePackageOrder(String line, String fileName) {
        try {
            // Extract year and month from file name
            String yearMonth = fileName.substring(fileName.length() - 10, fileName.length() - 4); // e.g., "202404"
            String year = yearMonth.substring(0, 4);
            String month = yearMonth.substring(4, 6);

            String[] parts = line.split(",");
            if (parts.length < 4) {
                System.out.println("Invalid line: " + line);
                return;
            }

            // Parse date and time
            String dateTimeStr = parts[0].trim(); // "01 01:13"
            String dateStr = dateTimeStr + " " + month + " " + year; // "01 01:13 04 2024"
            SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm MM yyyy");
            Date orderDate = sdf.parse(dateStr);

            // Parse origin and destination UBIGEO codes
            String[] routeParts = parts[1].split("=>");
            if (routeParts.length != 2) {
                System.out.println("Invalid route format in line: " + line);
                return;
            }
            String originUbigeo = routeParts[0].trim();
            String destinationUbigeo = routeParts[1].trim();

            // Parse quantity
            int quantity = Integer.parseInt(parts[2].trim());

            // Parse order ID
            String orderId = parts[3].trim();

            // Find origin and destination cities
            City origin = cityRepository.findById(originUbigeo).orElse(null);
            City destination = cityRepository.findById(destinationUbigeo).orElse(null);

            if (origin == null) {
                System.out.println("Origin city not found for UBIGEO: " + originUbigeo);
                return;
            }
            if (destination == null) {
                System.out.println("Destination city not found for UBIGEO: " + destinationUbigeo);
                return;
            }

            // Create PackageOrder
            PackageOrder pkgOrder = new PackageOrder();
            pkgOrder.setOrderId(orderId);
            pkgOrder.setQuantity(quantity);
            pkgOrder.setOrigin(origin);
            pkgOrder.setDestination(destination);
            pkgOrder.setOrderDate(orderDate);
            pkgOrder.setDeliveryDeadline(calculateDeadline(destination.getRegion(), orderDate));

            packageOrderRepository.save(pkgOrder);

        } catch (ParseException e) {
            System.out.println("Parse error in line: " + line);
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Number format error in line: " + line);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error processing line: " + line);
            e.printStackTrace();
        }
    }

    private Resource[] getResources(String pattern) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        return resolver.getResources(pattern);
    }

    public void loadMaintenanceSchedule(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(":");
                String dateStr = tokens[0];
                String truckCode = tokens[1];
    
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                Date maintenanceStartDate = sdf.parse(dateStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(maintenanceStartDate);
                cal.add(Calendar.DAY_OF_MONTH, 2);
                Date maintenanceEndDate = cal.getTime();
    
                Truck truck = truckRepository.findById(truckCode).orElse(null);
                if (truck != null) {
                    truck.setUnderMaintenance(true);
                    truck.setMaintenanceStartTime(maintenanceStartDate);
                    truck.setMaintenanceEndTime(maintenanceEndDate);
                    truckRepository.save(truck);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    
    public void loadBlockages() {
        try {
            // Adjust the path to your data directory
            String dataDir = "classpath:data/";
            Resource[] resources = getResources(dataDir + "c.1inf54.24-2.bloqueo.*"); // Loads all blockage files

            for (Resource resource : resources) {
                logger.info("Loading blockage file: {}", resource.getFilename());
                loadBlockagesFromFile(resource);
            }
        } catch (IOException e) {
            logger.error("Error loading blockage files", e);
        }
    }

    /**
     * Loads a blockage file and applies the blockages to the road segments.
     * @param resource Resource object representing the blockage file.
     */
    public void loadBlockagesFromFile(Resource resource) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd,HH:mm");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                if (tokens.length != 2) {
                    logger.warn("Invalid line format: {}", line);
                    continue;
                }

                String roadSegmentPart = tokens[0].trim(); // "250301 => 220501"
                String blockagePeriodPart = tokens[1].trim(); // "0101,13:32==0119,10:39"

                // Parse road segment
                String[] roadTokens = roadSegmentPart.split("=>");
                if (roadTokens.length != 2) {
                    logger.warn("Invalid road segment format: {}", roadSegmentPart);
                    continue;
                }
                String originUbigeo = roadTokens[0].trim();
                String destinationUbigeo = roadTokens[1].trim();

                // Find origin and destination cities
                City origin = cityRepository.findById(originUbigeo).orElse(null);
                City destination = cityRepository.findById(destinationUbigeo).orElse(null);

                if (origin == null || destination == null) {
                    logger.warn("Cities not found for UBIGEOs: {} => {}", originUbigeo, destinationUbigeo);
                    continue;
                }

                // Find the road segment
                RoadSegment roadSegment = roadSegmentRepository.findByOriginAndDestination(origin, destination);
                if (roadSegment == null) {
                    logger.warn("Road segment not found: {} => {}", originUbigeo, destinationUbigeo);
                    continue;
                }

                // Parse blockage start and end times
                String[] dateTokens = blockagePeriodPart.split("==");
                if (dateTokens.length != 2) {
                    logger.warn("Invalid blockage period format: {}", blockagePeriodPart);
                    continue;
                }
                Date startDate = parseBlockageDate(dateTokens[0], dateFormat);
                Date endDate = parseBlockageDate(dateTokens[1], dateFormat);

                if (startDate != null && endDate != null) {
                    // Add blockage period to the road segment
                    roadSegment.addBlockagePeriod(startDate, endDate);
                    roadSegmentRepository.save(roadSegment);
                    logger.info("Added blockage to road segment {} => {} from {} to {}", originUbigeo, destinationUbigeo, startDate, endDate);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading blockage file: {}", resource.getFilename(), e);
        }
    }

    private Date parseBlockageDate(String dateStr, SimpleDateFormat dateFormat) {
        try {
            Calendar cal = Calendar.getInstance();
            Date date = dateFormat.parse(dateStr);
            cal.setTime(date);
            cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)); // Set current year
            return cal.getTime();
        } catch (ParseException e) {
            logger.error("Error parsing date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Loads scheduled breakdowns for each truck from the provided file.
     * The file format is:
     * 202403011230:A001:1
     * Where:
     * - `202403011230` is the date and time (yyyyMMddHHmm)
     * - `A001` is the truck code
     * - `1` is the breakdown type (1: Moderado, 2: Grave, 3: Siniestro)
     * 
     * @param filePath The path to the breakdown schedule file.
     */
    public void loadScheduledBreakdowns(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(":");
                if (tokens.length != 3) {
                    logger.warn("Invalid line format: {}", line);
                    continue;
                }

                // Parse date and time, truck code, and breakdown type
                String dateStr = tokens[0].trim();
                String truckCode = tokens[1].trim();
                int breakdownType = Integer.parseInt(tokens[2].trim());

                // Parse the breakdown start date and time
                Date breakdownStartDate = sdf.parse(dateStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(breakdownStartDate);

                // Assuming breakdown duration is 2 days
                cal.add(Calendar.DAY_OF_MONTH, 2);
                Date breakdownEndDate = cal.getTime();

                // Find the truck by its code
                Truck truck = truckRepository.findById(truckCode).orElse(null);
                if (truck != null) {
                    // Set the truck's breakdown details
                    truck.setBrokenDown(true);
                    truck.setBreakdownType(breakdownType);
                    truck.setBreakdownStartTime(breakdownStartDate);
                    truck.setBreakdownEndTime(breakdownEndDate);
                    truckRepository.save(truck);

                    logger.info("Scheduled breakdown for truck {} from {} to {} with type {}", 
                        truckCode, breakdownStartDate, breakdownEndDate, breakdownType);
                } else {
                    logger.warn("Truck with code {} not found", truckCode);
                }
            }
        } catch (IOException | ParseException e) {
            logger.error("Error loading scheduled breakdowns", e);
        }
    }
}
