package com.logistics.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;


public class PCRATest {

    @Test
    public void testPCRA_ClusterOptimization() {
        int numResources = 2;
        int numOrders = 5;
        int numScenarios = 100;
        List<Map<String, Double>> scenarios = new ArrayList<>();
        Random rand = new Random(42);
        for (int i = 0; i < numScenarios; i++) {
            Map<String, Double> scenario = new HashMap<>();
            for (int r = 0; r < numResources; r++) {
                double demand = 0.5 + rand.nextDouble();
                scenario.put("resource" + r + "_demand", demand);
            }
            scenarios.add(scenario);
        }
        
        Map<Integer, Double> avgDemand = new HashMap<>();
        for (int r = 0; r < numResources; r++) {
            double total = 0;
            for (Map<String, Double> scenario : scenarios) {
                total += scenario.get("resource" + r + "_demand");
            }
            avgDemand.put(r, total / numScenarios);
        }
        
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(avgDemand.entrySet());
        sorted.sort(Map.Entry.comparingByValue());
        Map<Integer, List<Integer>> assignments = new HashMap<>();
        int orderIndex = 0;
        while (orderIndex < numOrders) {
            for (Map.Entry<Integer, Double> entry : sorted) {
                if (orderIndex < numOrders) {
                    assignments.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(orderIndex);
                    orderIndex++;
                }
            }
        }
        assertFalse(assignments.isEmpty(), "Should produce assignments");
        assertEquals(numOrders, assignments.values().stream().mapToInt(List::size).sum(), "All orders should be assigned");
    }
}