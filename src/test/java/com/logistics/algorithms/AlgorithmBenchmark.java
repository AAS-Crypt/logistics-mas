package com.logistics.algorithms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;

public class AlgorithmBenchmark {

    @Test
    public void testAlgorithmComparison() {
        Order order = new Order();
        order.setOrderId("BENCH-001");
        order.setPriority(1);
        order.setDeadline(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
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
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 72 * 3600 * 1000));
        p2.setReliability(0.8);
        proposals.add(p2);

        Proposal p3 = new Proposal();
        p3.setOrder(order);
        p3.setPrice(750);
        p3.setEstimatedDelivery(new Date(System.currentTimeMillis() + 36 * 3600 * 1000));
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
        
        int rrBest = RoundRobinAllocator.selectProposal(proposals);
        int randomBest = RandomAllocator.selectProposal(proposals);
        int fcfsBest = FCFSAllocator.selectProposal(proposals);

        assertTrue(mcaaBest >= 0 && mcaaBest < proposals.size(), "MCAA should select valid proposal");
        assertTrue(rrBest >= 0 && rrBest < proposals.size(), "Round Robin should select valid proposal");
        assertTrue(randomBest >= 0 && randomBest < proposals.size(), "Random should select valid proposal");
        assertTrue(fcfsBest >= 0 && fcfsBest < proposals.size(), "FCFS should select valid proposal");

        System.out.println("=== Algorithm Benchmark ===");
        System.out.println("MCAA selected: Proposal " + (mcaaBest + 1) + " (score: " + mcaaScores.get(mcaaBest) + ")");
        System.out.println("Round Robin selected: Proposal " + (rrBest + 1));
        System.out.println("Random selected: Proposal " + (randomBest + 1));
        System.out.println("FCFS selected: Proposal " + (fcfsBest + 1));
    }

    @Test
    public void testMCAA_OptimalSelection() {
        Order order = new Order();
        order.setOrderId("OPTIMAL-001");
        order.setPriority(1);
        order.setDeadline(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        List<Proposal> proposals = new ArrayList<>();
        
        Proposal p1 = new Proposal();
        p1.setOrder(order);
        p1.setPrice(100);
        p1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 12 * 3600 * 1000));
        p1.setReliability(0.9);
        proposals.add(p1);

        Proposal p2 = new Proposal();
        p2.setOrder(order);
        p2.setPrice(50);
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
        p2.setReliability(0.8);
        proposals.add(p2);

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }
        assertTrue(scores.get(0) > scores.get(1),  "MCAA should prefer faster delivery (Proposal 1)");
    }
}