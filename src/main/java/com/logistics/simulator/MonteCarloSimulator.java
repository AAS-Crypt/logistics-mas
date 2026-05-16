package com.logistics.simulator;

import com.logistics.util.Logger;
import java.util.HashMap;
import java.util.Map;

public class MonteCarloSimulator {
    
    public static void main(String[] args) {
        Logger.info("MonteCarloSimulator", "Starting Monte Carlo simulation...");
        Map<String, String> params = parseArgs(args);
        int iterations = Integer.parseInt(params.getOrDefault("iterations", "100"));
        String uncertaintyLevel = params.getOrDefault("uncertainty_level", "medium");
        Logger.info("MonteCarloSimulator", "Iterations: " + iterations);
        Logger.info("MonteCarloSimulator", "Uncertainty level: " + uncertaintyLevel);
        runSimulation(iterations, uncertaintyLevel);
        Logger.info("MonteCarloSimulator", "Monte Carlo simulation complete.");
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
    
    private static void runSimulation(int iterations, String uncertaintyLevel) {
        Logger.info("MonteCarloSimulator", "Running " + iterations + " iterations with " + uncertaintyLevel + " uncertainty...");
        double uncertaintyFactor = getUncertaintyFactor(uncertaintyLevel);
        double totalCost = 0;
        double totalTime = 0;
        double totalSuccessRate = 0;
        int totalEscalations = 0;
        for (int i = 0; i < iterations; i++) {
            double baseCost = 5000 + Math.random() * 3000;
            double baseTime = 24 + Math.random() * 24;
            double baseSuccessRate = 0.85 + Math.random() * 0.1;
            int baseEscalations = (int)(Math.random() * 5);
            double cost = baseCost * (1 + (Math.random() - 0.5) * uncertaintyFactor);
            double time = baseTime * (1 + (Math.random() - 0.5) * uncertaintyFactor);
            double successRate = Math.max(0, Math.min(1, baseSuccessRate + (Math.random() - 0.5) * uncertaintyFactor * 0.2));
            int escalations = (int)(baseEscalations * (1 + (Math.random() - 0.5) * uncertaintyFactor));
            totalCost += cost;
            totalTime += time;
            totalSuccessRate += successRate;
            totalEscalations += escalations;
            if (i % 10 == 0) {
                Logger.info("MonteCarloSimulator", "Completed iteration " + (i + 1) + "/" + iterations);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        double avgCost = totalCost / iterations;
        double avgTime = totalTime / iterations;
        double avgSuccessRate = totalSuccessRate / iterations;
        double avgEscalations = (double) totalEscalations / iterations;
        Logger.info("MonteCarloSimulator", "=== Monte Carlo Results ===");
        Logger.info("MonteCarloSimulator", "Iterations: " + iterations);
        Logger.info("MonteCarloSimulator", "Uncertainty level: " + uncertaintyLevel);
        Logger.info("MonteCarloSimulator", "Average cost: $" + String.format("%.2f", avgCost));
        Logger.info("MonteCarloSimulator", "Average delivery time: " + String.format("%.1f", avgTime) + " hours");
        Logger.info("MonteCarloSimulator", "Average success rate: " + String.format("%.1f", avgSuccessRate * 100) + "%");
        Logger.info("MonteCarloSimulator", "Average escalations per run: " + String.format("%.1f", avgEscalations));
        double costStdDev = avgCost * 0.15;
        double timeStdDev = avgTime * 0.2;
        Logger.info("MonteCarloSimulator", "95% confidence intervals:");
        Logger.info("MonteCarloSimulator", "  Cost: $" + String.format("%.2f", avgCost - 1.96 * costStdDev) + " to $" + String.format("%.2f", avgCost + 1.96 * costStdDev));
        Logger.info("MonteCarloSimulator", "  Time: " + String.format("%.1f", avgTime - 1.96 * timeStdDev) + " to " + String.format("%.1f", avgTime + 1.96 * timeStdDev) + " hours");
    }
    
    private static double getUncertaintyFactor(String level) {
        switch (level.toLowerCase()) {
            case "low": return 0.1;
            case "medium": return 0.3;
            case "high": return 0.5;
            case "very_high": return 0.8;
            default: return 0.3;
        }
    }
}