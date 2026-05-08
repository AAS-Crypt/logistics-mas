package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.config.ConfigLoader;
import java.util.Date;


public class MCAA {

     
    public static double computeScore(Order order, Proposal proposal) {
        
        double weightCost = ConfigLoader.getDouble("mcaa.weight.cost", 0.3);
        double weightTime = ConfigLoader.getDouble("mcaa.weight.time", 0.4);
        double weightReliability = ConfigLoader.getDouble("mcaa.weight.reliability", 0.3);

        
        double maxBudget = order.getMaxBudget();
        double price = proposal.getPrice();
        double costScore;
        if (maxBudget > 0) {
            
            
            costScore = Math.max(0, (maxBudget - price) / maxBudget);
        } else {
            
            costScore = 1.0 / (1.0 + price);
        }

        
        long timeRemaining = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
        double timeScore = 1.0 / (1.0 + Math.max(0, timeRemaining / 3600000.0));

        
        double reliabilityScore = proposal.getReliability();

        return weightCost * costScore + weightTime * timeScore + weightReliability * reliabilityScore;
    }
}