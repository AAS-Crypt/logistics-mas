package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import jade.core.AID;
import java.util.*;


public class FCFSAllocator {
    public static int selectProposal(List<Proposal> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return -1;
        }
        return 0; 
    }
    public static Map<Integer, Double> computeScores(List<Proposal> proposals) {
        Map<Integer, Double> scores = new HashMap<>();
        
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, 1.0 / (i + 1.0)); 
        }
        return scores;
    }
}