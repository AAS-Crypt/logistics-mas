package com.logistics.test;

import com.logistics.ontology.predicates.Proposal;
import java.util.Date;

public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("=== Simple Proposal Test ===");
        Proposal proposal = new Proposal();
        proposal.setPrice(100.0);
        proposal.setReliability(0.9);
        long deliveryTime = System.currentTimeMillis() + 3600000; 
        proposal.setEstimatedDelivery(new Date(deliveryTime));
        System.out.println("Price: " + proposal.getPrice());
        System.out.println("Reliability: " + proposal.getReliability());
        System.out.println("Estimated Delivery: " + proposal.getEstimatedDelivery());
        System.out.println("Estimated Delivery Time: " + proposal.getEstimatedDelivery().getTime());
        System.out.println("Test completed successfully!");
    }
}