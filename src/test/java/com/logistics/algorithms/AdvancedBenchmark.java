package com.logistics.algorithms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;

public class AdvancedBenchmark {

    @Test
    public void testAllAlgorithms_Comparison() {
        Order order = new Order();
        order.setOrderId("ADV-BENCH-001");
        order.setPriority(1);
        order.setDeadline(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));

        List<Proposal> proposals = new ArrayList<>();
        
        Proposal p1 = new Proposal();
        p1.setOrder(order);
        p1.setPrice(1000);
        p1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 12 * 3600 * 1000));
        p1.setReliability(0.9);
        proposals.add(p1);

        Proposal p2 = new Proposal();
        p2.setOrder(order);
        p2.setPrice(500);
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
        p2.setReliability(0.8);
        proposals.add(p2);

        Proposal p3 = new Proposal();
        p3.setOrder(order);
        p3.setPrice(750);
        p3.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        p3.setReliability(0.85);
        proposals.add(p3);

        Map<Integer, Double> mcaaScores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            mcaaScores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }
        int mcaaBest = mcaaScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1);

        Map<Integer, List<Proposal>> proposalsMap = new HashMap<>();
        proposalsMap.put(0, proposals);
        List<Order> orders = new ArrayList<>();
        orders.add(order);
        Map<Integer, Integer> gaResult = GeneticAlgorithm.optimize(orders, proposalsMap);
        Map<Integer, Integer> saResult = SimulatedAnnealing.optimize(orders, proposalsMap);
        Map<Integer, Integer> psoResult = ParticleSwarmOptimization.optimize(orders, proposalsMap);

        int rrBest = RoundRobinAllocator.selectProposal(proposals);
        int randomBest = RandomAllocator.selectProposal(proposals);
        int fcfsBest = FCFSAllocator.selectProposal(proposals);

        assertTrue(mcaaBest >= 0 && mcaaBest < proposals.size());
        assertTrue(gaResult.get(0) >= 0 && gaResult.get(0) < proposals.size());
        assertTrue(saResult.get(0) >= 0 && saResult.get(0) < proposals.size());
        assertTrue(psoResult.get(0) >= 0 && psoResult.get(0) < proposals.size());
        assertTrue(rrBest >= 0 && rrBest < proposals.size());
        assertTrue(randomBest >= 0 && randomBest < proposals.size());
        assertTrue(fcfsBest >= 0 && fcfsBest < proposals.size());

        System.out.println("=== Advanced Algorithm Benchmark ===");
        System.out.println("MCAA selected: Proposal " + (mcaaBest + 1) + " (score: " + mcaaScores.get(mcaaBest) + ")");
        System.out.println("GA selected: Proposal " + (gaResult.get(0) + 1));
        System.out.println("SA selected: Proposal " + (saResult.get(0) + 1));
        System.out.println("PSO selected: Proposal " + (psoResult.get(0) + 1));
        System.out.println("Round Robin selected: Proposal " + (rrBest + 1));
        System.out.println("Random selected: Proposal " + (randomBest + 1));
        System.out.println("FCFS selected: Proposal " + (fcfsBest + 1));
    }

    @Test
    public void testAlgorithmPerformance_Scalability() {
        int numOrders = 10;
        int numResources = 5;

        List<Order> orders = new ArrayList<>();
        Map<Integer, List<Proposal>> proposalsMap = new HashMap<>();

        Random rand = new Random(42);
        for (int i = 0; i < numOrders; i++) {
            Order order = new Order();
            order.setOrderId("SCALE-" + i);
            order.setPriority(rand.nextInt(3) + 1);
            order.setDeadline(new Date(System.currentTimeMillis() + (24 + rand.nextInt(48)) * 3600 * 1000));
            orders.add(order);

            List<Proposal> proposals = new ArrayList<>();
            for (int j = 0; j < numResources; j++) {
                Proposal p = new Proposal();
                p.setOrder(order);
                p.setPrice(500 + rand.nextInt(1000));
                p.setEstimatedDelivery(new Date(System.currentTimeMillis() + (12 + rand.nextInt(36)) * 3600 * 1000));
                p.setReliability(0.7 + rand.nextDouble() * 0.3);
                proposals.add(p);
            }
            proposalsMap.put(i, proposals);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numOrders; i++) {
            for (Proposal p : proposalsMap.get(i)) {
                MCAA.computeScore(orders.get(i), p);
            }
        }
        long mcaaTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        GeneticAlgorithm.optimize(orders, proposalsMap);
        long gaTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        SimulatedAnnealing.optimize(orders, proposalsMap);
        long saTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        ParticleSwarmOptimization.optimize(orders, proposalsMap);
        long psoTime = System.currentTimeMillis() - startTime;

        System.out.println("=== Scalability Test (10 orders, 5 resources) ===");
        System.out.println("MCAA time: " + mcaaTime + "ms");
        System.out.println("GA time: " + gaTime + "ms");
        System.out.println("SA time: " + saTime + "ms");
        System.out.println("PSO time: " + psoTime + "ms");

        assertTrue(mcaaTime < gaTime, "MCAA should be faster than GA");
        assertTrue(mcaaTime < saTime, "MCAA should be faster than SA");
        assertTrue(mcaaTime < psoTime, "MCAA should be faster than PSO");
    }
}