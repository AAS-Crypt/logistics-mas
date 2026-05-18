package com.logistics.test;

import com.logistics.algorithms.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.test.data.OlistDataLoader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MCAA_OlistTest {

    private static final String[] ALGORITHMS = {"MCAA", "Vickrey", "DoubleAuction", "LP"};
    private static final String DATA_DIR = "data/Brazilian E-Commerce Public Dataset by Olist";
    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static void main(String[] args) {
        int sampleSize = Integer.getInteger("sample.size", 0);
        long seed = Long.getLong("seed", 42L);
        String timestamp = TS_FMT.format(new Date());
        System.out.println("=== MCAA Olist E-Commerce Test ===");
        System.out.printf("Sample size: %s | Seed: %d%n",
                sampleSize > 0 ? String.valueOf(sampleSize) : "ALL", seed);
        System.out.println("==================================\n");
        try {
            System.out.print("Loading Olist orders... ");
            List<Order> orders = OlistDataLoader.loadOrders(DATA_DIR, sampleSize);
            System.out.println(orders.size() + " orders loaded.");
            System.out.print("Loading Olist proposals... ");
            Map<Integer, List<Proposal>> proposals = OlistDataLoader.loadProposals(orders, DATA_DIR);
            System.out.println("proposals generated.");

            String csvPath = "mcaa_test_reports/" + timestamp + "_olist_results.csv";
            try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
                writeCsvHeader(csv);
                System.out.println("\nRunning algorithms...");
                for (String algo : ALGORITHMS) {
                    RunResult r = runAlgorithm(algo, orders, proposals, seed);
                    writeCsvRow(csv, seed, orders.size(), algo, "olist", r);
                    System.out.printf("  %-14s  Svc=%.4f  Cost=%.0f  Time=%dms  Gini=%.4f%n",
                            algo, r.serviceLevel, r.totalCost, r.executionTimeMs, r.giniCoefficient);
                }
            }
            System.out.println("\nResults written to: " + csvPath);

        } catch (Exception e) {
            System.err.println("MCAA_OlistTest failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static final int MAX_ORDERS = Integer.getInteger("max.orders", 2000);

    static RunResult runAlgorithm(String algoName, List<Order> orders,
                                   Map<Integer, List<Proposal>> proposals, long seed) {
        int originalSize = orders.size();
        if (MAX_ORDERS > 0 && orders.size() > MAX_ORDERS) {
            orders = new ArrayList<>(orders.subList(0, MAX_ORDERS));
            Map<Integer, List<Proposal>> capped = new HashMap<>();
            for (int i = 0; i < MAX_ORDERS; i++) {
                capped.put(i, deepCopyEntry(proposals.get(i)));
            }
            proposals = capped;
        }
        Map<Integer, List<Proposal>> local = deepCopy(proposals);
        long t0 = System.currentTimeMillis();
        Map<Integer, Integer> alloc;

        switch (algoName) {
            case "MCAA":  alloc = runMCAA(orders, local); break;
            case "Vickrey": alloc = VickreyAuction.allocate(orders, local); break;
            case "DoubleAuction": alloc = DoubleAuction.allocate(orders, local); break;
            case "LP": alloc = LinearProgrammingSolver.allocate(orders, local); break;
            default: throw new IllegalArgumentException("Unknown: " + algoName);
        }
        long t = System.currentTimeMillis() - t0;
        RunResult r = computeMetrics(orders, local, alloc, t);
        if (MAX_ORDERS > 0 && originalSize > MAX_ORDERS) {
            r.numOrders = MAX_ORDERS;
        }
        return r;
    }

    private static List<Proposal> deepCopyEntry(List<Proposal> orig) {
        if (orig == null) return Collections.emptyList();
        List<Proposal> copy = new ArrayList<>();
        for (Proposal p : orig) {
            Proposal cp = new Proposal();
            cp.setPrice(p.getPrice());
            cp.setReliability(p.getReliability());
            cp.setEstimatedDelivery(p.getEstimatedDelivery() != null
                    ? new Date(p.getEstimatedDelivery().getTime()) : null);
            cp.setOrder(p.getOrder());
            cp.setResource(p.getResource());
            copy.add(cp);
        }
        return copy;
    }

    static Map<Integer, Integer> runMCAA(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> alloc = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            Order ord = orders.get(i);
            int bestIdx = 0;
            double bestScore = -1;
            for (int j = 0; j < plist.size(); j++) {
                double s = MCAA.computeScore(ord, plist.get(j));
                if (s > bestScore) { bestScore = s; bestIdx = j; }
            }
            alloc.put(i, bestIdx);
        }
        return alloc;
    }

    static RunResult computeMetrics(List<Order> orders, Map<Integer, List<Proposal>> proposals,
                                     Map<Integer, Integer> alloc, long execMs) {
        RunResult r = new RunResult();
        r.executionTimeMs = execMs;
        int onTime = 0;
        List<Double> costs = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Integer idx = alloc.get(i);
            if (idx == null) continue;
            List<Proposal> plist = proposals.get(i);
            if (plist == null || idx >= plist.size()) continue;
            Proposal p = plist.get(idx);
            Order o = orders.get(i);
            r.totalCost += p.getPrice();
            costs.add(p.getPrice());
            if (o.getDeadline() != null && p.getEstimatedDelivery() != null) {
                if (p.getEstimatedDelivery().getTime() <= o.getDeadline().getTime()) onTime++;
            }
        }
        r.serviceLevel = orders.size() > 0 ? (double) onTime / orders.size() : 0;
        r.giniCoefficient = computeGini(costs);
        r.numOrders = orders.size();
        return r;
    }

    static double computeGini(List<Double> vals) {
        if (vals.size() <= 1) return 0;
        Collections.sort(vals);
        int n = vals.size();
        double sum = 0, total = 0;
        for (int i = 0; i < n; i++) {
            sum += vals.get(i) * (i + 1);
            total += vals.get(i);
        }
        if (total == 0) return 0;
        return Math.max(0, (2.0 * sum) / (n * total) - (n + 1.0) / n);
    }

    static Map<Integer, List<Proposal>> deepCopy(Map<Integer, List<Proposal>> orig) {
        Map<Integer, List<Proposal>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<Proposal>> e : orig.entrySet()) {
            List<Proposal> clist = new ArrayList<>();
            for (Proposal p : e.getValue()) {
                Proposal cp = new Proposal();
                cp.setPrice(p.getPrice());
                cp.setReliability(p.getReliability());
                cp.setEstimatedDelivery(p.getEstimatedDelivery() != null
                        ? new Date(p.getEstimatedDelivery().getTime()) : null);
                cp.setOrder(p.getOrder());
                cp.setResource(p.getResource());
                clist.add(cp);
            }
            copy.put(e.getKey(), clist);
        }
        return copy;
    }

    static void writeCsvHeader(PrintWriter csv) {
        csv.println("seed,dataset,numOrders,algorithm,serviceLevel,totalCost,executionTimeMs,giniCoefficient");
    }

    static void writeCsvRow(PrintWriter csv, long seed, int n, String algo, String dataset, RunResult r) {
        csv.printf(Locale.US, "%d,%s,%d,%s,%.4f,%.2f,%d,%.4f%n",
                seed, dataset, r.numOrders > 0 ? r.numOrders : n, algo,
                r.serviceLevel, r.totalCost, r.executionTimeMs, r.giniCoefficient);
    }

    static class RunResult {
        double serviceLevel, totalCost, giniCoefficient;
        long executionTimeMs;
        int numOrders;
    }
}