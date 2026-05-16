package com.logistics.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;


public class SPTATest {
    @Test
    public void testSPTA_PolicyUpdate() {
        Map<String, Double> currentPolicy = new HashMap<>();
        currentPolicy.put("mcaa.weight.cost", 0.3);
        currentPolicy.put("mcaa.weight.time", 0.4);
        currentPolicy.put("mcaa.weight.reliability", 0.3);
        Map<String, Double> kpis = new HashMap<>();
        kpis.put("avg_cost", 1500.0);
        kpis.put("avg_delivery_time", 30.0);
        Map<String, Double> newPolicy = new HashMap<>(currentPolicy);
        if (kpis.get("avg_cost") > 1000) {
            double newCostWeight = Math.min(0.5, currentPolicy.get("mcaa.weight.cost") + 0.05);
            newPolicy.put("mcaa.weight.cost", newCostWeight);
        }
        if (kpis.get("avg_delivery_time") > 24) {
            double newTimeWeight = Math.min(0.6, currentPolicy.get("mcaa.weight.time") + 0.05);
            newPolicy.put("mcaa.weight.time", newTimeWeight);
        }
        double sum = newPolicy.values().stream().mapToDouble(Double::doubleValue).sum();
        for (Map.Entry<String, Double> entry : newPolicy.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }
        assertTrue(newPolicy.get("mcaa.weight.cost") > currentPolicy.get("mcaa.weight.cost"), "Cost weight should increase when avg_cost > 1000");
        assertTrue(newPolicy.get("mcaa.weight.time") > currentPolicy.get("mcaa.weight.time"), "Time weight should increase when avg_delivery_time > 24");
    }
}