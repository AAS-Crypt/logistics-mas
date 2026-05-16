package com.logistics.algorithms;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

import java.util.*;

public class LinearProgrammingSolver {

    public static Map<Integer, Integer> allocate(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        try {
            return allocateWithOjAlgo(orders, proposals);
        } catch (Throwable t) {
            System.err.println("ojAlgo LP failed: " + t.toString() + " — using fallback");
            return allocateGreedy(orders, proposals);
        }
    }

    private static Map<Integer, Integer> allocateWithOjAlgo(List<Order> orders,
                                                             Map<Integer, List<Proposal>> proposals) {
        int N = orders.size();
        Map<Integer, Map<Integer, Integer>> varIndex = new LinkedHashMap<>();
        List<double[]> varData = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            varIndex.put(i, new LinkedHashMap<>());
            Order order = orders.get(i);
            for (int j = 0; j < plist.size(); j++) {
                varIndex.get(i).put(j, varData.size());
                Proposal p = plist.get(j);
                double cost = p.getPrice();
                if (order.getDeadline() != null && p.getEstimatedDelivery() != null) {
                    long etaMs = p.getEstimatedDelivery().getTime();
                    long deadlineMs = order.getDeadline().getTime();
                    if (etaMs > deadlineMs) {
                        double days = (etaMs - deadlineMs) / (24.0 * 3600_000.0);
                        cost += cost * Math.min(1.0, days * 0.5);
                    }
                }
                varData.add(new double[]{cost, (double) i, (double) j});
            }
        }

        if (varData.isEmpty()) return new LinkedHashMap<>();
        int numVars = varData.size();
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        for (int k = 0; k < numVars; k++) {
            model.addVariable();
            model.getVariable(k).binary().lower(0).upper(1);
        }
        model.addExpression("objective").weight(1.0);
        for (int k = 0; k < numVars; k++) {
            model.getExpression("objective").set(k, varData.get(k)[0]);
        }
        for (int i = 0; i < N; i++) {
            Map<Integer, Integer> idxMap = varIndex.get(i);
            if (idxMap == null || idxMap.isEmpty()) continue;
            model.addExpression("order_" + i).level(1.0);
            for (int k : idxMap.values()) {
                model.getExpression("order_" + i).set(k, 1.0);
            }
        }
        model.minimise();
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
        for (int k = 0; k < numVars; k++) {
            double val = model.getVariable(k).getValue().doubleValue();
            if (val > 0.5) {
                double[] data = varData.get(k);
                allocation.put((int) data[1], (int) data[2]);
            }
        }
        for (int i = 0; i < N; i++) {
            if (allocation.containsKey(i)) continue;
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            allocation.put(i, 0);
        }

        return allocation;
    }

    private static Map<Integer, Integer> allocateGreedy(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            Order order = orders.get(i);

            int bestIdx = 0;
            double bestCost = Double.MAX_VALUE;

            for (int j = 0; j < plist.size(); j++) {
                Proposal p = plist.get(j);
                double cost = p.getPrice();
                if (order.getDeadline() != null && p.getEstimatedDelivery() != null) {
                    long etaMs = p.getEstimatedDelivery().getTime();
                    long deadlineMs = order.getDeadline().getTime();
                    if (etaMs > deadlineMs) {
                        double days = (etaMs - deadlineMs) / (24.0 * 3600_000.0);
                        cost += cost * Math.min(1.0, days * 0.5);
                    }
                }
                if (cost < bestCost) {
                    bestCost = cost;
                    bestIdx = j;
                }
            }
            if (bestCost < Double.MAX_VALUE) {
                allocation.put(i, bestIdx);
            }
        }
        return allocation;
    }
}