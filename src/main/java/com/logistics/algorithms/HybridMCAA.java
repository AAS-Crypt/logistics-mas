package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.config.ConfigLoader;
import java.util.*;


public class HybridMCAA {
    
     
    public enum HybridStrategy {
        MCAA_GA,      
        MCAA_SA,      
        MCAA_PSO,     
        ADAPTIVE      
    }
    
     
    public static Map<Integer, Integer> optimizeMCAA_GA(List<Order> orders, 
                                                         Map<Integer, List<Proposal>> proposals) {
        
        Map<Integer, Integer> initialSolution = getMCAASolution(orders, proposals);
        
        
        Map<Integer, Integer> refinedSolution = refineWithGA(orders, proposals, initialSolution);
        
        return refinedSolution;
    }
    
     
    public static Map<Integer, Integer> optimizeMCAA_SA(List<Order> orders, 
                                                         Map<Integer, List<Proposal>> proposals) {
        
        Map<Integer, Integer> initialSolution = getMCAASolution(orders, proposals);
        
        
        Map<Integer, Integer> refinedSolution = refineWithSA(orders, proposals, initialSolution);
        
        return refinedSolution;
    }
    
     
    public static Map<Integer, Integer> optimizeMCAA_PSO(List<Order> orders, 
                                                          Map<Integer, List<Proposal>> proposals) {
        
        Map<Integer, Integer> initialSolution = getMCAASolution(orders, proposals);
        
        
        Map<Integer, Integer> refinedSolution = refineWithPSO(orders, proposals, initialSolution);
        
        return refinedSolution;
    }
    
     
    public static Map<Integer, Integer> optimizeAdaptive(List<Order> orders, 
                                                          Map<Integer, List<Proposal>> proposals) {
        
        int numOrders = orders.size();
        int totalProposals = proposals.values().stream().mapToInt(List::size).sum();
        double avgProposalsPerOrder = (double) totalProposals / numOrders;
        
        
        HybridStrategy strategy;
        if (numOrders < 10 && avgProposalsPerOrder < 5) {
            
            strategy = HybridStrategy.MCAA_SA;
        } else if (numOrders > 50 || avgProposalsPerOrder > 10) {
            
            strategy = HybridStrategy.MCAA_GA;
        } else {
            
            strategy = HybridStrategy.MCAA_PSO;
        }
        
        
        switch (strategy) {
            case MCAA_GA:
                return optimizeMCAA_GA(orders, proposals);
            case MCAA_SA:
                return optimizeMCAA_SA(orders, proposals);
            case MCAA_PSO:
                return optimizeMCAA_PSO(orders, proposals);
            default:
                return optimizeMCAA_GA(orders, proposals);
        }
    }
    
     
    private static Map<Integer, Integer> getMCAASolution(List<Order> orders, 
                                                          Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> solution = new HashMap<>();
        
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                int bestIdx = 0;
                double bestScore = -1;
                Order order = orders.get(i);
                
                for (int j = 0; j < orderProposals.size(); j++) {
                    double score = MCAA.computeScore(order, orderProposals.get(j));
                    if (score > bestScore) {
                        bestScore = score;
                        bestIdx = j;
                    }
                }
                
                solution.put(i, bestIdx);
            }
        }
        
        return solution;
    }
    
     
    private static Map<Integer, Integer> refineWithGA(List<Order> orders, 
                                                       Map<Integer, List<Proposal>> proposals,
                                                       Map<Integer, Integer> initialSolution) {
        
        
        Map<Integer, Integer> refined = GeneticAlgorithm.optimize(orders, proposals);
        
        
        double initialScore = calculateTotalScore(orders, proposals, initialSolution);
        double refinedScore = calculateTotalScore(orders, proposals, refined);
        
        return refinedScore > initialScore ? refined : initialSolution;
    }
    
     
    private static Map<Integer, Integer> refineWithSA(List<Order> orders, 
                                                       Map<Integer, List<Proposal>> proposals,
                                                       Map<Integer, Integer> initialSolution) {
        
        Map<Integer, Integer> refined = SimulatedAnnealing.optimize(orders, proposals);
        
        
        double initialScore = calculateTotalScore(orders, proposals, initialSolution);
        double refinedScore = calculateTotalScore(orders, proposals, refined);
        
        return refinedScore > initialScore ? refined : initialSolution;
    }
    
     
    private static Map<Integer, Integer> refineWithPSO(List<Order> orders, 
                                                        Map<Integer, List<Proposal>> proposals,
                                                        Map<Integer, Integer> initialSolution) {
        
        Map<Integer, Integer> refined = ParticleSwarmOptimization.optimize(orders, proposals);
        
        
        double initialScore = calculateTotalScore(orders, proposals, initialSolution);
        double refinedScore = calculateTotalScore(orders, proposals, refined);
        
        return refinedScore > initialScore ? refined : initialSolution;
    }
    
     
    private static double calculateTotalScore(List<Order> orders, 
                                             Map<Integer, List<Proposal>> proposals,
                                             Map<Integer, Integer> allocation) {
        double totalScore = 0;
        
        for (int i = 0; i < orders.size(); i++) {
            Integer resourceIndex = allocation.get(i);
            List<Proposal> orderProposals = proposals.get(i);
            
            if (resourceIndex != null && orderProposals != null && resourceIndex < orderProposals.size()) {
                Proposal proposal = orderProposals.get(resourceIndex);
                totalScore += MCAA.computeScore(orders.get(i), proposal);
            }
        }
        
        return totalScore;
    }
    
     
    public static Map<Integer, Double> computeScores(List<Proposal> proposals, HybridStrategy strategy) {
        Map<Integer, Double> scores = new HashMap<>();
        
        
        for (int i = 0; i < proposals.size(); i++) {
            
            Order dummyOrder = new Order();
            scores.put(i, MCAA.computeScore(dummyOrder, proposals.get(i)));
        }
        
        return scores;
    }
}