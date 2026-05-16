package com.logistics.test.data;

import com.logistics.ontology.concepts.Location;
import java.util.*;

public class RealWorldTestData {
    public static final Map<String, Location> CITIES = new HashMap<>();
    public static final Map<String, Map<String, Double>> DISTANCES = new HashMap<>();
    public static final List<String> WAREHOUSES = Arrays.asList(
        "Chicago", "Los Angeles", "Dallas", "Atlanta", "Seattle"
    );
    public static final List<String> CUSTOMER_LOCATIONS = Arrays.asList(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose",
        "Austin", "Jacksonville", "Fort Worth", "Columbus", "Charlotte",
        "Seattle", "Denver", "Boston", "Nashville", "Portland"
    );
    static {
        CITIES.put("New York", createLocation("New York", 40.7128, -74.0060));
        CITIES.put("Los Angeles", createLocation("Los Angeles", 34.0522, -118.2437));
        CITIES.put("Chicago", createLocation("Chicago", 41.8781, -87.6298));
        CITIES.put("Houston", createLocation("Houston", 29.7604, -95.3698));
        CITIES.put("Phoenix", createLocation("Phoenix", 33.4484, -112.0740));
        CITIES.put("Philadelphia", createLocation("Philadelphia", 39.9526, -75.1652));
        CITIES.put("San Antonio", createLocation("San Antonio", 29.4241, -98.4936));
        CITIES.put("San Diego", createLocation("San Diego", 32.7157, -117.1611));
        CITIES.put("Dallas", createLocation("Dallas", 32.7767, -96.7970));
        CITIES.put("San Jose", createLocation("San Jose", 37.3382, -121.8863));
        CITIES.put("Austin", createLocation("Austin", 30.2672, -97.7431));
        CITIES.put("Jacksonville", createLocation("Jacksonville", 30.3322, -81.6557));
        CITIES.put("Fort Worth", createLocation("Fort Worth", 32.7555, -97.3308));
        CITIES.put("Columbus", createLocation("Columbus", 39.9612, -82.9988));
        CITIES.put("Charlotte", createLocation("Charlotte", 35.2271, -80.8431));
        CITIES.put("Seattle", createLocation("Seattle", 47.6062, -122.3321));
        CITIES.put("Denver", createLocation("Denver", 39.7392, -104.9903));
        CITIES.put("Boston", createLocation("Boston", 42.3601, -71.0589));
        CITIES.put("Nashville", createLocation("Nashville", 36.1627, -86.7816));
        CITIES.put("Portland", createLocation("Portland", 45.5152, -122.6784));
        CITIES.put("Atlanta", createLocation("Atlanta", 33.7490, -84.3880));
        initializeDistances();
    }
    
    private static void initializeDistances() {
        Map<String, Double> fromChicago = new HashMap<>();
        fromChicago.put("New York", 1260.0);
        fromChicago.put("Los Angeles", 2800.0);
        fromChicago.put("Chicago", 0.0);
        fromChicago.put("Houston", 1650.0);
        fromChicago.put("Phoenix", 2400.0);
        fromChicago.put("Philadelphia", 1120.0);
        fromChicago.put("San Antonio", 1800.0);
        fromChicago.put("San Diego", 2800.0);
        fromChicago.put("Dallas", 1450.0);
        fromChicago.put("San Jose", 3000.0);
        fromChicago.put("Austin", 1700.0);
        fromChicago.put("Jacksonville", 1400.0);
        fromChicago.put("Fort Worth", 1450.0);
        fromChicago.put("Columbus", 530.0);
        fromChicago.put("Charlotte", 1050.0);
        fromChicago.put("Seattle", 2800.0);
        fromChicago.put("Denver", 1500.0);
        fromChicago.put("Boston", 1380.0);
        fromChicago.put("Nashville", 650.0);
        fromChicago.put("Portland", 2800.0);
        fromChicago.put("Atlanta", 1050.0);
        DISTANCES.put("Chicago", fromChicago);
        
        Map<String, Double> fromLA = new HashMap<>();
        fromLA.put("New York", 3950.0);
        fromLA.put("Los Angeles", 0.0);
        fromLA.put("Chicago", 2800.0);
        fromLA.put("Houston", 2200.0);
        fromLA.put("Phoenix", 570.0);
        fromLA.put("Philadelphia", 3850.0);
        fromLA.put("San Antonio", 1950.0);
        fromLA.put("San Diego", 190.0);
        fromLA.put("Dallas", 2000.0);
        fromLA.put("San Jose", 540.0);
        fromLA.put("Austin", 1950.0);
        fromLA.put("Jacksonville", 3500.0);
        fromLA.put("Fort Worth", 2000.0);
        fromLA.put("Columbus", 3400.0);
        fromLA.put("Charlotte", 3500.0);
        fromLA.put("Seattle", 1550.0);
        fromLA.put("Denver", 1500.0);
        fromLA.put("Boston", 4200.0);
        fromLA.put("Nashville", 2800.0);
        fromLA.put("Portland", 1350.0);
        fromLA.put("Atlanta", 3100.0);
        DISTANCES.put("Los Angeles", fromLA);
        
        Map<String, Double> fromDallas = new HashMap<>();
        fromDallas.put("New York", 2200.0);
        fromDallas.put("Los Angeles", 2000.0);
        fromDallas.put("Chicago", 1450.0);
        fromDallas.put("Houston", 360.0);
        fromDallas.put("Phoenix", 1400.0);
        fromDallas.put("Philadelphia", 2100.0);
        fromDallas.put("San Antonio", 400.0);
        fromDallas.put("San Diego", 1900.0);
        fromDallas.put("Dallas", 0.0);
        fromDallas.put("San Jose", 2300.0);
        fromDallas.put("Austin", 300.0);
        fromDallas.put("Jacksonville", 1500.0);
        fromDallas.put("Fort Worth", 50.0);
        fromDallas.put("Columbus", 1600.0);
        fromDallas.put("Charlotte", 1600.0);
        fromDallas.put("Seattle", 3000.0);
        fromDallas.put("Denver", 1200.0);
        fromDallas.put("Boston", 2500.0);
        fromDallas.put("Nashville", 1000.0);
        fromDallas.put("Portland", 2800.0);
        fromDallas.put("Atlanta", 1200.0);
        DISTANCES.put("Dallas", fromDallas);
        
        Map<String, Double> fromAtlanta = new HashMap<>();
        fromAtlanta.put("New York", 1200.0);
        fromAtlanta.put("Los Angeles", 3100.0);
        fromAtlanta.put("Chicago", 1050.0);
        fromAtlanta.put("Houston", 1100.0);
        fromAtlanta.put("Phoenix", 2600.0);
        fromAtlanta.put("Philadelphia", 1050.0);
        fromAtlanta.put("San Antonio", 1300.0);
        fromAtlanta.put("San Diego", 3000.0);
        fromAtlanta.put("Dallas", 1200.0);
        fromAtlanta.put("San Jose", 3400.0);
        fromAtlanta.put("Austin", 1200.0);
        fromAtlanta.put("Jacksonville", 550.0);
        fromAtlanta.put("Fort Worth", 1200.0);
        fromAtlanta.put("Columbus", 700.0);
        fromAtlanta.put("Charlotte", 400.0);
        fromAtlanta.put("Seattle", 3600.0);
        fromAtlanta.put("Denver", 2000.0);
        fromAtlanta.put("Boston", 1500.0);
        fromAtlanta.put("Nashville", 400.0);
        fromAtlanta.put("Portland", 3500.0);
        fromAtlanta.put("Atlanta", 0.0);
        DISTANCES.put("Atlanta", fromAtlanta);
        
        Map<String, Double> fromSeattle = new HashMap<>();
        fromSeattle.put("New York", 4500.0);
        fromSeattle.put("Los Angeles", 1550.0);
        fromSeattle.put("Chicago", 2800.0);
        fromSeattle.put("Houston", 3200.0);
        fromSeattle.put("Phoenix", 1800.0);
        fromSeattle.put("Philadelphia", 4400.0);
        fromSeattle.put("San Antonio", 3100.0);
        fromSeattle.put("San Diego", 1700.0);
        fromSeattle.put("Dallas", 3000.0);
        fromSeattle.put("San Jose", 1100.0);
        fromSeattle.put("Austin", 3000.0);
        fromSeattle.put("Jacksonville", 4200.0);
        fromSeattle.put("Fort Worth", 3000.0);
        fromSeattle.put("Columbus", 3500.0);
        fromSeattle.put("Charlotte", 4100.0);
        fromSeattle.put("Seattle", 0.0);
        fromSeattle.put("Denver", 2000.0);
        fromSeattle.put("Boston", 4800.0);
        fromSeattle.put("Nashville", 3300.0);
        fromSeattle.put("Portland", 280.0);
        fromSeattle.put("Atlanta", 3600.0);
        DISTANCES.put("Seattle", fromSeattle);
    }
    
    public static double getDistance(String from, String to) {
        if (from.equals(to)) return 0.0;
        Map<String, Double> fromDistances = DISTANCES.get(from);
        if (fromDistances != null && fromDistances.containsKey(to)) {
            return fromDistances.get(to);
        }
        Map<String, Double> toDistances = DISTANCES.get(to);
        if (toDistances != null && toDistances.containsKey(from)) {
            return toDistances.get(from);
        }
        Location loc1 = CITIES.get(from);
        Location loc2 = CITIES.get(to);
        if (loc1 != null && loc2 != null) {
            return haversineDistance(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
        }
        return 1000.0; 
    }
    
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; 
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    public static String getRandomCustomerLocation(Random rand) {
        return CUSTOMER_LOCATIONS.get(rand.nextInt(CUSTOMER_LOCATIONS.size()));
    }
    
    public static String getRandomWarehouse(Random rand) {
        return WAREHOUSES.get(rand.nextInt(WAREHOUSES.size()));
    }
    
    public static List<String> getAllCities() {
        return new ArrayList<>(CITIES.keySet());
    }
    
    private static Location createLocation(String city, double latitude, double longitude) {
        Location location = new Location();
        location.setCity(city);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
