package com.logistics.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ConfigLoader {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties = null;

     
    public static Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream(CONFIG_FILE)) {
                if (input == null) {
                    System.err.println("Unable to find " + CONFIG_FILE);
                    
                    loadDefaults();
                    return properties;
                }
                properties.load(input);
            } catch (IOException ex) {
                System.err.println("Error loading configuration: " + ex.getMessage());
                loadDefaults();
            }
        }
        return properties;
    }

     
    private static void loadDefaults() {
        properties.setProperty("mcaa.weight.cost", "0.3");
        properties.setProperty("mcaa.weight.time", "0.4");
        properties.setProperty("mcaa.weight.reliability", "0.3");
        properties.setProperty("order.maxRetries", "5");
        properties.setProperty("order.retryIntervalMs", "2000");
        properties.setProperty("monitor.eventGenerationIntervalMs", "15000");
        properties.setProperty("aera.threshold.low", "0.3");
        properties.setProperty("aera.threshold.high", "0.7");
        properties.setProperty("auction.responseTimeoutMs", "10000");
    }

     
    public static String getString(String key) {
        return getProperties().getProperty(key);
    }

     
    public static double getDouble(String key, double defaultValue) {
        String value = getProperties().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid double value for " + key + ": " + value);
            return defaultValue;
        }
    }

     
    public static int getInt(String key, int defaultValue) {
        String value = getProperties().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for " + key + ": " + value);
            return defaultValue;
        }
    }

     
    public static long getLong(String key, long defaultValue) {
        String value = getProperties().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid long value for " + key + ": " + value);
            return defaultValue;
        }
    }
}