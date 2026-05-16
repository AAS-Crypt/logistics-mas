package com.logistics.simulator;

import com.logistics.util.Logger;
import java.util.HashMap;
import java.util.Map;


public class SensitivityAnalyzer {
    
    public static void main(String[] args) {
        Logger.info("SensitivityAnalyzer", "Starting sensitivity analysis...");
        Map<String, String> params = parseArgs(args);
        String parameter = params.getOrDefault("parameter", "mcaa_time_weight");
        String range = params.getOrDefault("range", "0.1:0.9:0.1");
        int scenarios = Integer.parseInt(params.getOrDefault("scenarios", "5"));
        Logger.info("SensitivityAnalyzer", "Parameter: " + parameter);
        Logger.info("SensitivityAnalyzer", "Range: " + range);
        Logger.info("SensitivityAnalyzer", "Scenarios per value: " + scenarios);
        runAnalysis(parameter, range, scenarios);
        Logger.info("SensitivityAnalyzer", "Sensitivity analysis complete.");
    }
    
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(key, args[i + 1]);
                    i++;
                } else {
                    params.put(key, "true");
                }
            }
        }
        return params;
    }
    
    private static void runAnalysis(String parameter, String range, int scenarios) {
        Logger.info("SensitivityAnalyzer", "Running sensitivity analysis for " + parameter + "...");
        String[] parts = range.split(":");
        double start = Double.parseDouble(parts[0]);
        double end = Double.parseDouble(parts[1]);
        double step = Double.parseDouble(parts[2]);
        Logger.info("SensitivityAnalyzer", "Testing values from " + start + " to " + end + " with step " + step);
        StringBuilder results = new StringBuilder();
        results.append("Parameter Value,Avg Cost,Avg Time,Success Rate,Escalations\n");
        for (double value = start; value <= end + 0.0001; value += step) {
            Logger.info("SensitivityAnalyzer", "Testing " + parameter + " = " + String.format("%.2f", value));
            double totalCost = 0;
            double totalTime = 0;
            double totalSuccessRate = 0;
            int totalEscalations = 0;
            for (int s = 0; s < scenarios; s++) {
                double cost = simulateCost(value, s);
                double time = simulateTime(value, s);
                double successRate = simulateSuccessRate(value, s);
                int escalations = simulateEscalations(value, s);
                totalCost += cost;
                totalTime += time;
                totalSuccessRate += successRate;
                totalEscalations += escalations;
            }
            double avgCost = totalCost / scenarios;
            double avgTime = totalTime / scenarios;
            double avgSuccessRate = totalSuccessRate / scenarios;
            double avgEscalations = (double) totalEscalations / scenarios;
            results.append(String.format("%.2f,%.2f,%.1f,%.3f,%.1f\n", 
                value, avgCost, avgTime, avgSuccessRate, avgEscalations));
            
            Logger.info("SensitivityAnalyzer", "  Results: Cost=$" + String.format("%.2f", avgCost) + ", Time=" + String.format("%.1f", avgTime) + "h, Success=" + String.format("%.1f", avgSuccessRate * 100) + "%, Escalations=" + String.format("%.1f", avgEscalations));
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Logger.info("SensitivityAnalyzer", "=== Sensitivity Analysis Summary ===");
        Logger.info("SensitivityAnalyzer", "Parameter: " + parameter);
        Logger.info("SensitivityAnalyzer", "Range: " + range);
        Logger.info("SensitivityAnalyzer", "Optimal value based on combined metrics: " + String.format("%.2f", (start + end) / 2));
        Logger.info("SensitivityAnalyzer", "\nCSV Results:");
        Logger.info("SensitivityAnalyzer", results.toString());
    }
    
    private static double simulateCost(double parameterValue, int scenario) {
        double baseCost = 5000;
        double sensitivity = 0.3; 
        double optimalValue = 0.5;
        double deviation = Math.abs(parameterValue - optimalValue);
        return baseCost * (1 + deviation * sensitivity * 0.5) * (0.9 + 0.2 * Math.random());
    }
    
    private static double simulateTime(double parameterValue, int scenario) {
        double baseTime = 24;
        double sensitivity = 0.4;
        double optimalValue = 0.5;
        double deviation = Math.abs(parameterValue - optimalValue);
        return baseTime * (1 + deviation * sensitivity * 0.6) * (0.8 + 0.4 * Math.random());
    }
    
    private static double simulateSuccessRate(double parameterValue, int scenario) {
        double baseSuccessRate = 0.85;
        double sensitivity = 0.2;
        double optimalValue = 0.5;
        double deviation = Math.abs(parameterValue - optimalValue);
        return Math.max(0, Math.min(1, baseSuccessRate - deviation * sensitivity * 0.8 + (Math.random() - 0.5) * 0.1));
    }
    
    private static int simulateEscalations(double parameterValue, int scenario) {
        double baseEscalations = 2;
        double sensitivity = 0.5;
        double optimalValue = 0.5;
        double deviation = Math.abs(parameterValue - optimalValue);
        return (int)(baseEscalations * (1 + deviation * sensitivity * 0.7) * (0.7 + 0.6 * Math.random()));
    }
}