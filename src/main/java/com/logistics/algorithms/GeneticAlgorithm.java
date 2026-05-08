package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;


public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 50;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static class Chromosome {
        int[] genes; 
        double fitness;
        Chromosome(int numOrders) {
            genes = new int[numOrders];
            Random rand = new Random();
            for (int i = 0; i < numOrders; i++) {
                genes[i] = rand.nextInt(numOrders);
            }
        }
        Chromosome(int[] genes) {
            this.genes = genes.clone();
        }
    }

    public static Map<Integer, Integer> optimize(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        List<Chromosome> population = initializePopulation(orders.size(), proposals);
        for (Chromosome chromosome : population) {
            chromosome.fitness = calculateFitness(chromosome, orders, proposals);
        }
        for (int generation = 0; generation < GENERATIONS; generation++) {
            List<Chromosome> selected = tournamentSelection(population);
            List<Chromosome> offspring = crossover(selected);
            mutate(offspring, proposals);
            for (Chromosome chromosome : offspring) {
                chromosome.fitness = calculateFitness(chromosome, orders, proposals);
            }
            population = offspring;
        }
        Chromosome best = Collections.max(population, Comparator.comparingDouble(c -> c.fitness));
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            result.put(i, best.genes[i]);
        }
        return result;
    }
     
    private static List<Chromosome> initializePopulation(int numOrders, Map<Integer, List<Proposal>> proposals) {
        List<Chromosome> population = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = new Chromosome(numOrders);
            for (int j = 0; j < numOrders; j++) {
                List<Proposal> orderProposals = proposals.get(j);
                if (orderProposals != null && !orderProposals.isEmpty()) {
                    chromosome.genes[j] = rand.nextInt(orderProposals.size());
                }
            }
            population.add(chromosome);
        }
        return population;
    }
     
    private static double calculateFitness(Chromosome chromosome, List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        double totalCost = 0;
        double totalTime = 0;
        double totalReliability = 0;
        for (int i = 0; i < orders.size(); i++) {
            int resourceIndex = chromosome.genes[i];
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && resourceIndex < orderProposals.size()) {
                Proposal proposal = orderProposals.get(resourceIndex);
                totalCost += proposal.getPrice();
                long deliveryTime = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                totalTime += Math.max(0, deliveryTime / 3600000.0);
                totalReliability += proposal.getReliability();
            }
        }
        
        double avgCost = totalCost / orders.size();
        double avgTime = totalTime / orders.size();
        double avgReliability = totalReliability / orders.size();
        return 1.0 / (1.0 + avgCost / 1000 + avgTime / 24) + avgReliability;
    }
     
    private static List<Chromosome> tournamentSelection(List<Chromosome> population) {
        List<Chromosome> selected = new ArrayList<>();
        Random rand = new Random();
        int tournamentSize = 5;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Chromosome> tournament = new ArrayList<>();
            for (int j = 0; j < tournamentSize; j++) {
                tournament.add(population.get(rand.nextInt(population.size())));
            }
            Chromosome winner = Collections.max(tournament, Comparator.comparingDouble(c -> c.fitness));
            selected.add(new Chromosome(winner.genes));
        }
        return selected;
    }
     
    private static List<Chromosome> crossover(List<Chromosome> population) {
        List<Chromosome> offspring = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < population.size(); i += 2) {
            if (i + 1 < population.size()) {
                Chromosome parent1 = population.get(i);
                Chromosome parent2 = population.get(i + 1);
                if (rand.nextDouble() < CROSSOVER_RATE) {
                    int crossoverPoint = rand.nextInt(parent1.genes.length);
                    int[] child1Genes = new int[parent1.genes.length];
                    int[] child2Genes = new int[parent1.genes.length];
                    for (int j = 0; j < crossoverPoint; j++) {
                        child1Genes[j] = parent1.genes[j];
                        child2Genes[j] = parent2.genes[j];
                    }
                    for (int j = crossoverPoint; j < parent1.genes.length; j++) {
                        child1Genes[j] = parent2.genes[j];
                        child2Genes[j] = parent1.genes[j];
                    }
                    offspring.add(new Chromosome(child1Genes));
                    offspring.add(new Chromosome(child2Genes));
                } else {
                    offspring.add(new Chromosome(parent1.genes));
                    offspring.add(new Chromosome(parent2.genes));
                }
            }
        }
        return offspring;
    }

    private static void mutate(List<Chromosome> population, Map<Integer, List<Proposal>> proposals) {
        Random rand = new Random();
        for (Chromosome chromosome : population) {
            for (int i = 0; i < chromosome.genes.length; i++) {
                if (rand.nextDouble() < MUTATION_RATE) {
                    List<Proposal> orderProposals = proposals.get(i);
                    if (orderProposals != null && !orderProposals.isEmpty()) {
                        chromosome.genes[i] = rand.nextInt(orderProposals.size());
                    }
                }
            }
        }
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