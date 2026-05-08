package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;


public class SimulatedAnnealing {
    private static final double INITIAL_TEMPERATURE = 1000.0;
    private static final double COOLING_RATE = 0.995;
    private static final int ITERATIONS = 1000;
    private static final double MIN_TEMPERATURE = 0.1;

     
    public static Map<Integer, Integer> optimize(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Random rand = new Random();
        
        
        Map<Integer, Integer> currentSolution = initializeSolution(orders, proposals);
        double currentEnergy = calculateEnergy(currentSolution, orders, proposals);
        
        
        Map<Integer, Integer> bestSolution = new HashMap<>(currentSolution);
        double bestEnergy = currentEnergy;
        
        double temperature = INITIAL_TEMPERATURE;
        
        
        for (int iteration = 0; iteration < ITERATIONS && temperature > MIN_TEMPERATURE; iteration++) {
            
            Map<Integer, Integer> neighbor = generateNeighbor(currentSolution, orders, proposals);
            double neighborEnergy = calculateEnergy(neighbor, orders, proposals);
            
            
            double deltaE = neighborEnergy - currentEnergy;
            
            
            if (deltaE < 0 || rand.nextDouble() < Math.exp(-deltaE / temperature)) {
                currentSolution = neighbor;
                currentEnergy = neighborEnergy;
                
                
                if (currentEnergy < bestEnergy) {
                    bestSolution = new HashMap<>(currentSolution);
                    bestEnergy = currentEnergy;
                }
            }
            
            
            temperature *= COOLING_RATE;
        }
        
        return bestSolution;
    }

     
    private static Map<Integer, Integer> initializeSolution(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> solution = new HashMap<>();
        Random rand = new Random();
        
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                solution.put(i, rand.nextInt(orderProposals.size()));
            }
        }
        
        return solution;
    }

     
    private static double calculateEnergy(Map<Integer, Integer> solution, List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        double totalCost = 0;
        double totalTime = 0;
        double totalReliability = 0;
        
        for (int i = 0; i < orders.size(); i++) {
            Integer resourceIndex = solution.get(i);
            List<Proposal> orderProposals = proposals.get(i);
            
            if (resourceIndex != null && orderProposals != null && resourceIndex < orderProposals.size()) {
                Proposal proposal = orderProposals.get(resourceIndex);
                totalCost += proposal.getPrice();
                
                long deliveryTime = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                totalTime += Math.max(0, deliveryTime / 3600000.0);
                
                totalReliability += proposal.getReliability();
            }
        }
        
        
        return totalCost / 1000 + totalTime / 24 - totalReliability;
    }

     
    private static Map<Integer, Integer> generateNeighbor(Map<Integer, Integer> current, List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> neighbor = new HashMap<>(current);
        Random rand = new Random();
        
        
        int orderIndex = rand.nextInt(orders.size());
        List<Proposal> orderProposals = proposals.get(orderIndex);
        
        if (orderProposals != null && !orderProposals.isEmpty()) {
            
            neighbor.put(orderIndex, rand.nextInt(orderProposals.size()));
        }
        
        return neighbor;
    }

     
    public static Map<Integer, Double> computeScores(List<Proposal> proposals) {
        Map<Integer, Double> scores = new HashMap<>();
        Random rand = new Random();
        
        for (int i = 0; i < proposals.size(); i++) {
            
            scores.put(i, rand.nextDouble());
        }
        
        return scores;
    }
}