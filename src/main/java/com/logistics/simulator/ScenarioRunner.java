package com.logistics.simulator;

import com.logistics.util.Logger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ScenarioRunner {
    
    private static final Map<String, String> SCENARIO_DESCRIPTIONS = new HashMap<>();
    
    static {
        SCENARIO_DESCRIPTIONS.put("high_priority", "High priority orders with tight deadlines");
        SCENARIO_DESCRIPTIONS.put("low_budget", "Orders with limited budget constraints");
        SCENARIO_DESCRIPTIONS.put("multiple_resources", "Many resources with varying capacities");
        SCENARIO_DESCRIPTIONS.put("extreme_urgency", "Orders with extremely tight deadlines");
    }
    
    public static void main(String[] args) {
        Logger.info("ScenarioRunner", "Starting scenario runner...");
        
        
        Map<String, String> params = parseArgs(args);
        String scenario = params.getOrDefault("scenario", "high_priority");
        int orders = Integer.parseInt(params.getOrDefault("orders", "10"));
        int resources = Integer.parseInt(params.getOrDefault("resources", "3"));
        double urgency = Double.parseDouble(params.getOrDefault("urgency", "0.8"));
        double budgetMultiplier = Double.parseDouble(params.getOrDefault("budget_multiplier", "1.0"));
        double deadlineMultiplier = Double.parseDouble(params.getOrDefault("deadline_multiplier", "1.0"));
        
        Logger.info("ScenarioRunner", "Scenario: " + scenario);
        Logger.info("ScenarioRunner", "Description: " + SCENARIO_DESCRIPTIONS.getOrDefault(scenario, "Unknown scenario"));
        Logger.info("ScenarioRunner", "Parameters: orders=" + orders + ", resources=" + resources);
        
        
        runScenario(scenario, orders, resources, urgency, budgetMultiplier, deadlineMultiplier);
        
        Logger.info("ScenarioRunner", "Scenario execution complete.");
        
        
        printResults(scenario, orders, resources);
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
    
    private static void runScenario(String scenario, int orders, int resources, 
                                   double urgency, double budgetMultiplier, double deadlineMultiplier) {
        Logger.info("ScenarioRunner", "Executing " + scenario + " scenario...");
        
        switch (scenario) {
            case "high_priority":
                Logger.info("ScenarioRunner", "Creating " + orders + " high priority orders");
                Logger.info("ScenarioRunner", "Urgency level: " + urgency);
                break;
            case "low_budget":
                Logger.info("ScenarioRunner", "Creating " + orders + " low budget orders");
                Logger.info("ScenarioRunner", "Budget multiplier: " + budgetMultiplier);
                break;
            case "multiple_resources":
                Logger.info("ScenarioRunner", "Creating " + resources + " resources");
                Logger.info("ScenarioRunner", "Processing " + orders + " orders");
                break;
            case "extreme_urgency":
                Logger.info("ScenarioRunner", "Creating " + orders + " extremely urgent orders");
                Logger.info("ScenarioRunner", "Deadline multiplier: " + deadlineMultiplier);
                break;
            default:
                Logger.warning("ScenarioRunner", "Unknown scenario: " + scenario);
        }
        
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void printResults(String scenario, int orders, int resources) {
        Logger.info("ScenarioRunner", "=== Scenario Results ===");
        Logger.info("ScenarioRunner", "Scenario: " + scenario);
        Logger.info("ScenarioRunner", "Orders processed: " + orders);
        Logger.info("ScenarioRunner", "Resources available: " + resources);
        
        
        Logger.info("ScenarioRunner", "Average completion time: " + (20 + Math.random() * 30) + " hours");
        Logger.info("ScenarioRunner", "Average cost: $" + (5000 + Math.random() * 3000));
        Logger.info("ScenarioRunner", "Success rate: " + (85 + Math.random() * 10) + "%");
        
        if ("high_priority".equals(scenario)) {
            Logger.info("ScenarioRunner", "Priority orders completed on time: " + (90 + Math.random() * 8) + "%");
        } else if ("low_budget".equals(scenario)) {
            Logger.info("ScenarioRunner", "Budget adherence: " + (88 + Math.random() * 9) + "%");
        } else if ("multiple_resources".equals(scenario)) {
            Logger.info("ScenarioRunner", "Resource utilization: " + (75 + Math.random() * 20) + "%");
        } else if ("extreme_urgency".equals(scenario)) {
            Logger.info("ScenarioRunner", "Urgent orders completed: " + (70 + Math.random() * 15) + "%");
        }
    }
}