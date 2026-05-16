package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.util.*;

public class DoubleAuction {

    public static Map<Integer, Integer> allocate(List<Order> orders,
                                                  Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
        Set<Integer> allocatedOrders = new HashSet<>();
        List<BidAskPair> book = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null) continue;
            Order order = orders.get(i);
            double bidPrice = order.getMaxBudget() > 0 ? order.getMaxBudget() : 5000;
            for (int j = 0; j < plist.size(); j++) {
                Proposal p = plist.get(j);
                double askPrice = p.getPrice();
                book.add(new BidAskPair(i, j, bidPrice, askPrice));
            }
        }
        book.sort((a, b) -> Double.compare(b.bidPrice, a.bidPrice));
        Set<String> usedResources = new HashSet<>();
        for (BidAskPair pair : book) {
            if (allocatedOrders.contains(pair.orderIdx)) continue;
            String resId = "R-" + pair.orderIdx + "-" + pair.propIdx;
            if (usedResources.contains(resId)) continue;
            if (pair.bidPrice >= pair.askPrice) {
                allocation.put(pair.orderIdx, pair.propIdx);
                allocatedOrders.add(pair.orderIdx);
                usedResources.add(resId);
                List<Proposal> plist = proposals.get(pair.orderIdx);
                if (plist != null && pair.propIdx < plist.size()) {
                    double clearingPrice = (pair.bidPrice + pair.askPrice) / 2.0;
                    plist.get(pair.propIdx).setPrice(clearingPrice);
                }
            }
        }
        for (int i = 0; i < orders.size(); i++) {
            if (allocatedOrders.contains(i)) continue;
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            int bestIdx = 0;
            double bestPrice = Double.MAX_VALUE;
            for (int j = 0; j < plist.size(); j++) {
                String resId = "R-" + i + "-" + j;
                if (!usedResources.contains(resId) && plist.get(j).getPrice() < bestPrice) {
                    bestPrice = plist.get(j).getPrice();
                    bestIdx = j;
                }
            }
            if (bestPrice < Double.MAX_VALUE) {
                allocation.put(i, bestIdx);
                usedResources.add("R-" + i + "-" + bestIdx);
            }
        }
        return allocation;
    }

    private static class BidAskPair {
        final int orderIdx;
        final int propIdx;
        final double bidPrice;
        final double askPrice;

        BidAskPair(int orderIdx, int propIdx, double bidPrice, double askPrice) {
            this.orderIdx = orderIdx;
            this.propIdx = propIdx;
            this.bidPrice = bidPrice;
            this.askPrice = askPrice;
        }
    }
}