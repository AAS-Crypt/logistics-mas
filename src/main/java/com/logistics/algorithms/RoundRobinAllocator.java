package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import jade.core.AID;
import java.util.*;


public class RoundRobinAllocator {
    private static int currentIndex = 0;

     
    public static int selectProposal(List<Proposal> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return -1;
        }
        
        int selected = currentIndex % proposals.size();
        currentIndex++;
        return selected;
    }

     
    public static Map<Integer, Double> computeScores(List<Proposal> proposals) {
        Map<Integer, Double> scores = new HashMap<>();
        int selected = selectProposal(proposals);
        
        for (int i = 0; i < proposals.size(); i++) {
            scores.put(i, (i == selected) ? 1.0 : 0.0);
        }
        
        return scores;
    }
}