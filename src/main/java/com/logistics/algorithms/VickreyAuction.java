package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.util.*;

public class VickreyAuction {
    public static Map<Integer, Integer> allocate(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            List<ProposalWithIndex> sorted = new ArrayList<>();
            for (int j = 0; j < plist.size(); j++) {
                sorted.add(new ProposalWithIndex(j, plist.get(j)));
            }
            sorted.sort(Comparator.comparingDouble(a -> a.proposal.getPrice()));
            int winnerIdx = sorted.get(0).index;
            double vickreyPrice;
            if (sorted.size() >= 2) {
                vickreyPrice = sorted.get(1).proposal.getPrice();
            } else {
                vickreyPrice = sorted.get(0).proposal.getPrice();
            }
            sorted.get(0).proposal.setPrice(vickreyPrice);
            allocation.put(i, winnerIdx);
        }
        return allocation;
    }

    private static class ProposalWithIndex {
        final int index;
        final Proposal proposal;
        ProposalWithIndex(int index, Proposal proposal) {
            this.index = index;
            this.proposal = proposal;
        }
    }
}