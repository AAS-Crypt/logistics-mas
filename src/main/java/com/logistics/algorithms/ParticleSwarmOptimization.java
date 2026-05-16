package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;

public class ParticleSwarmOptimization {
    private static final int NUM_PARTICLES = 30;
    private static final int MAX_ITERATIONS = 100;
    private static final double INERTIA_WEIGHT = 0.7;
    private static final double COGNITIVE_WEIGHT = 1.5;
    private static final double SOCIAL_WEIGHT = 1.5;

    private static class Particle {
        int[] position; 
        int[] velocity;
        int[] bestPosition;
        double fitness;
        double bestFitness;

        Particle(int numOrders) {
            position = new int[numOrders];
            velocity = new int[numOrders];
            bestPosition = new int[numOrders];
            
            Random rand = new Random();
            for (int i = 0; i < numOrders; i++) {
                position[i] = rand.nextInt(numOrders);
                velocity[i] = rand.nextInt(3) - 1; 
                bestPosition[i] = position[i];
            }
        }
    }

    public static Map<Integer, Integer> optimize(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(orders.size()));
        }
        int[] globalBestPosition = new int[orders.size()];
        double globalBestFitness = Double.MAX_VALUE;
        for (Particle particle : particles) {
            particle.fitness = calculateFitness(particle.position, orders, proposals);
            particle.bestFitness = particle.fitness;
            if (particle.fitness < globalBestFitness) {
                globalBestFitness = particle.fitness;
                globalBestPosition = particle.position.clone();
            }
        }
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            for (Particle particle : particles) {
                Random rand = new Random();
                for (int i = 0; i < orders.size(); i++) {
                    int cognitive = rand.nextInt(2) * (particle.bestPosition[i] - particle.position[i]);
                    int social = rand.nextInt(2) * (globalBestPosition[i] - particle.position[i]);
                    particle.velocity[i] = (int) (INERTIA_WEIGHT * particle.velocity[i] + COGNITIVE_WEIGHT * cognitive + SOCIAL_WEIGHT * social);
                    particle.velocity[i] = Math.max(-5, Math.min(5, particle.velocity[i]));
                }
                for (int i = 0; i < orders.size(); i++) {
                    particle.position[i] += particle.velocity[i];
                    List<Proposal> orderProposals = proposals.get(i);
                    if (orderProposals != null && !orderProposals.isEmpty()) {
                        particle.position[i] = Math.max(0, Math.min(orderProposals.size() - 1, particle.position[i]));
                    }
                }
                particle.fitness = calculateFitness(particle.position, orders, proposals);
                if (particle.fitness < particle.bestFitness) {
                    particle.bestFitness = particle.fitness;
                    particle.bestPosition = particle.position.clone();
                }
                if (particle.fitness < globalBestFitness) {
                    globalBestFitness = particle.fitness;
                    globalBestPosition = particle.position.clone();
                }
            }
        }
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            result.put(i, globalBestPosition[i]);
        }
        return result;
    }

    private static double calculateFitness(int[] position, List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        double totalCost = 0;
        double totalTime = 0;
        double totalReliability = 0;
        int validOrders = 0;
        for (int i = 0; i < orders.size(); i++) {
            int resourceIndex = position[i];
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty() && 
                resourceIndex >= 0 && resourceIndex < orderProposals.size()) {
                Proposal proposal = orderProposals.get(resourceIndex);
                totalCost += proposal.getPrice();
                long deliveryTime = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                totalTime += Math.max(0, deliveryTime / 3600000.0);
                totalReliability += proposal.getReliability();
                validOrders++;
            } else if (orderProposals != null && !orderProposals.isEmpty()) {
                Proposal proposal = orderProposals.get(0);
                totalCost += proposal.getPrice();
                long deliveryTime = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                totalTime += Math.max(0, deliveryTime / 3600000.0);
                totalReliability += proposal.getReliability();
                validOrders++;
            }
        }
        if (validOrders == 0) {
            return Double.MAX_VALUE; 
        }
        double avgCost = totalCost / validOrders;
        double avgTime = totalTime / validOrders;
        double avgReliability = totalReliability / validOrders;
        return avgCost / 1000 + avgTime / 24 - avgReliability;
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