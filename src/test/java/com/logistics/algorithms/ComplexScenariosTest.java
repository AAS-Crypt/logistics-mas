package com.logistics.algorithms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.*;


public class ComplexScenariosTest {

    @Test
    public void testScenario_HighPriorityOrder() {
        Order order = new Order();
        order.setOrderId("HIGH-PRIORITY-001");
        order.setPriority(1); 
        order.setDeadline(new Date(System.currentTimeMillis() + 6 * 3600 * 1000)); 

        List<Proposal> proposals = new ArrayList<>();
        
        
        Proposal p1 = new Proposal();
        p1.setOrder(order);
        p1.setPrice(2000);
        p1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 4 * 3600 * 1000));
        p1.setReliability(0.95);
        proposals.add(p1);

        
        Proposal p2 = new Proposal();
        p2.setOrder(order);
        p2.setPrice(800);
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 12 * 3600 * 1000));
        p2.setReliability(0.85);
        proposals.add(p2);

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }

        
        assertTrue(scores.get(0) > scores.get(1), 
            "For urgent order, MCAA should prefer faster delivery");
    }

    @Test
    public void testScenario_LowBudgetOrder() {
        Order order = new Order();
        order.setOrderId("LOW-BUDGET-001");
        order.setPriority(3); 
        order.setDeadline(new Date(System.currentTimeMillis() + 72 * 3600 * 1000)); 
        order.setMaxBudget(600); 

        List<Proposal> proposals = new ArrayList<>();
        
        
        Proposal p1 = new Proposal();
        p1.setOrder(order);
        p1.setPrice(1000);
        p1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        p1.setReliability(0.9);
        proposals.add(p1);

        
        Proposal p2 = new Proposal();
        p2.setOrder(order);
        p2.setPrice(500);
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
        p2.setReliability(0.8);
        proposals.add(p2);

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }

        
        assertTrue(scores.get(1) > scores.get(0), 
            "For low budget order, MCAA should prefer cheaper option");
    }

    @Test
    public void testScenario_MultipleResources() {
        Order order = new Order();
        order.setOrderId("MULTI-RESOURCE-001");
        order.setPriority(2);
        order.setDeadline(new Date(System.currentTimeMillis() + 36 * 3600 * 1000));

        List<Proposal> proposals = new ArrayList<>();
        
        
        for (int i = 0; i < 5; i++) {
            Proposal p = new Proposal();
            p.setOrder(order);
            p.setPrice(600 + i * 100); 
            p.setEstimatedDelivery(new Date(System.currentTimeMillis() + (24 - i * 2) * 3600 * 1000)); 
            p.setReliability(0.8 + i * 0.03); 
            proposals.add(p);
        }

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }

        
        assertEquals(5, scores.size(), "Should have scores for all proposals");
        
        
        for (Double score : scores.values()) {
            assertTrue(score >= 0 && score <= 1, "Score should be between 0 and 1");
        }
    }

    @Test
    public void testScenario_EdgeCase_ZeroTime() {
        Order order = new Order();
        order.setOrderId("ZERO-TIME-001");
        order.setPriority(1);
        order.setDeadline(new Date(System.currentTimeMillis() + 1000)); 

        List<Proposal> proposals = new ArrayList<>();
        
        Proposal p1 = new Proposal();
        p1.setOrder(order);
        p1.setPrice(100);
        p1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 500)); 
        p1.setReliability(0.9);
        proposals.add(p1);

        Proposal p2 = new Proposal();
        p2.setOrder(order);
        p2.setPrice(50);
        p2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 2000)); 
        p2.setReliability(0.8);
        proposals.add(p2);

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, MCAA.computeScore(order, proposals.get(i)));
        }

        
        assertTrue(scores.get(0) > scores.get(1), 
            "For extremely urgent order, MCAA should prefer faster delivery");
    }
}