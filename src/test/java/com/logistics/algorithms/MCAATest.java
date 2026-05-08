package com.logistics.algorithms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import java.util.Date;


public class MCAATest {

    private Order testOrder;

    @BeforeEach
    public void setUp() {
        
        testOrder = new Order();
        testOrder.setOrderId("TEST-ORD-001");
        testOrder.setPriority(1);
        testOrder.setDeadline(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
    }

     
    @Test
    public void testMCAAScoring_TimeWeightDominates() {
        
        Proposal proposal1 = new Proposal();
        proposal1.setOrder(testOrder);
        proposal1.setPrice(100);
        proposal1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal1.setReliability(0.9);

        
        Proposal proposal2 = new Proposal();
        proposal2.setOrder(testOrder);
        proposal2.setPrice(80);
        proposal2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 48 * 3600 * 1000));
        proposal2.setReliability(0.8);

        
        double score1 = MCAA.computeScore(testOrder, proposal1);
        double score2 = MCAA.computeScore(testOrder, proposal2);

        
        assertTrue(score1 > score2, 
            "Proposal 1 should have higher score due to shorter delivery time. " +
            "Score1=" + score1 + ", Score2=" + score2);
    }

     
    @Test
    public void testMCAAScoring_IdenticalProposals() {
        Proposal proposal1 = new Proposal();
        proposal1.setOrder(testOrder);
        proposal1.setPrice(100);
        proposal1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal1.setReliability(0.9);

        Proposal proposal2 = new Proposal();
        proposal2.setOrder(testOrder);
        proposal2.setPrice(100);
        proposal2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal2.setReliability(0.9);

        double score1 = MCAA.computeScore(testOrder, proposal1);
        double score2 = MCAA.computeScore(testOrder, proposal2);

        assertEquals(score1, score2, 0.001, 
            "Identical proposals should have identical scores");
    }

     
    @Test
    public void testMCAAScoring_HighReliabilityWins() {
        
        Proposal proposal1 = new Proposal();
        proposal1.setOrder(testOrder);
        proposal1.setPrice(100);
        proposal1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal1.setReliability(0.95);

        
        Proposal proposal2 = new Proposal();
        proposal2.setOrder(testOrder);
        proposal2.setPrice(100);
        proposal2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal2.setReliability(0.7);

        double score1 = MCAA.computeScore(testOrder, proposal1);
        double score2 = MCAA.computeScore(testOrder, proposal2);

        assertTrue(score1 > score2, 
            "Higher reliability should result in higher score");
    }

     
    @Test
    public void testMCAAScoring_LowPriceCanWin() {
        
        Proposal proposal1 = new Proposal();
        proposal1.setOrder(testOrder);
        proposal1.setPrice(1000);
        proposal1.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        proposal1.setReliability(0.9);

        
        Proposal proposal2 = new Proposal();
        proposal2.setOrder(testOrder);
        proposal2.setPrice(50);
        proposal2.setEstimatedDelivery(new Date(System.currentTimeMillis() + 72 * 3600 * 1000));
        proposal2.setReliability(0.9);

        double score1 = MCAA.computeScore(testOrder, proposal1);
        double score2 = MCAA.computeScore(testOrder, proposal2);

        
        
        assertTrue(score1 > score2, 
            "Faster delivery should win despite higher price with default weights");
    }
}