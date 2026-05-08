package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import jade.core.AID;
import java.util.*;


public class RandomAllocator {
    private static Random random = new Random();

     
    public static int selectProposal(List<Proposal> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return -1;
        }
        
        return random.nextInt(proposals.size());
    }

     
    public static Map<Integer, Double> computeScores(List<Proposal> proposals) {
        Map<Integer, Double> scores = new HashMap<>();
        
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, random.nextDouble());
        }
        
        return scores;
    }
}