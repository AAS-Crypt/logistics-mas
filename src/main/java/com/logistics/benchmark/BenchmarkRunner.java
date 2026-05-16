package com.logistics.benchmark;

import com.logistics.algorithms.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.config.ConfigLoader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.Locale;

public class BenchmarkRunner {
    private static int numOrders;
    private static int numResources;
    private static double etaVariance;
    private static String weightStrategy;
    private static long seed;
    private static int iterations;
    private static String outputPath;
    private static final double[] ETA_VARIANCES = {0.0, 0.1, 0.2, 0.3};
    private static final String[] WEIGHT_STRATEGIES = {"balanced", "costHeavy", "timeHeavy"};
    private static final String[] ALGORITHMS = {"MCAA", "Vickrey", "DoubleAuction", "LP"};
    public static void main(String[] args) {
        loadParameters();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        System.out.println("=== Benchmark Runner ===");
        System.out.printf("Orders: %d | Resources: %d | ETA Variance: %.2f | Weight: %s | Seed: %d | Iter: %d%n", numOrders, numResources, etaVariance, weightStrategy, seed, iterations);
        System.out.println("Algorithms: " + String.join(", ", ALGORITHMS));
        System.out.println("========================\n");
        try (PrintWriter csv = new PrintWriter(new FileWriter("mcaa_test_reports/" + timestamp + "_" + outputPath))) {
            writeCsvHeader(csv);
            for (double eta : ETA_VARIANCES) {
                for (String ws : WEIGHT_STRATEGIES) {
                    applyWeightStrategy(ws);
                    runParameterPoint(eta, ws, csv);
                }
            }
            System.out.println("\nResults written to: " + timestamp + "_" + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void loadParameters() {
        numOrders      = Integer.getInteger("benchmark.orders", 20);
        numResources   = Integer.getInteger("benchmark.resources", 8);
        etaVariance    = Double.parseDouble(System.getProperty("benchmark.etaVariance", "0.2"));
        weightStrategy = System.getProperty("benchmark.weightStrategy", "balanced");
        seed           = Long.getLong("benchmark.seed", 42L);
        iterations     = Integer.getInteger("benchmark.iterations", 30);
        outputPath     = System.getProperty("benchmark.output", "benchmark_results.csv");
    }
    private static void applyWeightStrategy(String ws) {
        switch (ws) {
            case "costHeavy":
                System.setProperty("mcaa.weight.cost", "0.6");
                System.setProperty("mcaa.weight.time", "0.2");
                System.setProperty("mcaa.weight.reliability", "0.2");
                break;
            case "timeHeavy":
                System.setProperty("mcaa.weight.cost", "0.2");
                System.setProperty("mcaa.weight.time", "0.6");
                System.setProperty("mcaa.weight.reliability", "0.2");
                break;
            case "balanced":
            default:
                System.setProperty("mcaa.weight.cost", "0.3");
                System.setProperty("mcaa.weight.time", "0.4");
                System.setProperty("mcaa.weight.reliability", "0.3");
                break;
        }
    }
    private static void writeCsvHeader(PrintWriter csv) {
        csv.println("seed,numOrders,numResources,etaVariance,weightStrategy,algorithm," + "serviceLevel,totalCost,executionTimeMs,giniCoefficient");
    }
    private static void writeCsvRow(PrintWriter csv, long seed, int nOrd, int nRes, double eta, String ws, String algo, double svc, double cost, long timeMs, double gini) {
        csv.printf(Locale.US, "%d,%d,%d,%.2f,%s,%s,%.4f,%.2f,%d,%.4f%n", seed, nOrd, nRes, eta, ws, algo, svc, cost, timeMs, gini);
    }
    private static void runParameterPoint(double eta, String ws, PrintWriter csv) {
        System.out.printf("--- etaVariance=%.2f, weight=%s ---%n", eta, ws);
        for (int iter = 0; iter < iterations; iter++) {
            long iterSeed = seed + iter * 1000;
            Random rng = new Random(iterSeed);
            List<Order> orders = generateOrders(rng);
            Map<Integer, List<Proposal>> proposals = generateProposals(orders, eta, rng);
            for (String algo : ALGORITHMS) {
                RunResult result = runAlgorithm(algo, orders, proposals, rng);
                writeCsvRow(csv, iterSeed, numOrders, numResources, eta, ws, algo, result.serviceLevel, result.totalCost, result.executionTimeMs, result.giniCoefficient);
            }
            if ((iter + 1) % 10 == 0) {
                System.out.printf("  %d/%d iterations done%n", iter + 1, iterations);
            }
        }
    }

    private static List<Order> generateOrders(Random rng) {
        List<Order> orders = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < numOrders; i++) {
            Order o = new Order();
            o.setOrderId("ORD-" + i);
            o.setPriority(1 + rng.nextInt(5));
            double budget = 5000 + rng.nextGaussian() * 2000;
            budget = Math.max(1000, Math.min(10000, budget));
            o.setMaxBudget(budget);
            long deadlineMs = now + (long)((24 + rng.nextDouble() * 96) * 3600_000L);
            o.setDeadline(new Date(deadlineMs));
            orders.add(o);
        }
        return orders;
    }

    private static Map<Integer, List<Proposal>> generateProposals(List<Order> orders, double eta, Random rng) {
        Map<Integer, List<Proposal>> map = new HashMap<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            List<Proposal> list = new ArrayList<>();
            int numBids = 2 + rng.nextInt(Math.min(4, numResources));
            for (int j = 0; j < numBids && j < numResources; j++) {
                Proposal p = new Proposal();
                double basePrice = order.getMaxBudget() * (0.3 + rng.nextDouble() * 0.5);
                p.setPrice(basePrice);
                p.setReliability(0.7 + rng.nextDouble() * 0.3);
                double baseEtaHours = 24 + rng.nextDouble() * 72;
                double noise = eta * baseEtaHours * rng.nextGaussian();
                double actualEtaHours = Math.max(1, baseEtaHours + noise);
                long deliveryMs = now + (long)(actualEtaHours * 3600_000L);
                p.setEstimatedDelivery(new Date(deliveryMs));
                list.add(p);
            }
            map.put(i, list);
        }
        return map;
    }

    private static RunResult runAlgorithm(String algoName, List<Order> orders, Map<Integer, List<Proposal>> proposals, Random rng) {
        Map<Integer, List<Proposal>> localProps = deepCopyProposals(proposals);
        long t0 = System.currentTimeMillis();
        Map<Integer, Integer> allocation;
        switch (algoName) {
            case "MCAA":
                allocation = runMCAAAllocation(orders, localProps);
                break;
            case "Vickrey":
                allocation = VickreyAuction.allocate(orders, localProps);
                break;
            case "DoubleAuction":
                allocation = DoubleAuction.allocate(orders, localProps);
                break;
            case "LP":
                allocation = LinearProgrammingSolver.allocate(orders, localProps);
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algoName);
        }
        long execTimeMs = System.currentTimeMillis() - t0;
        return computeMetrics(orders, localProps, allocation, execTimeMs);
    }

    private static Map<Integer, Integer> runMCAAAllocation(List<Order> orders, Map<Integer, List<Proposal>> proposals) {
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
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
            allocation.put(i, bestIdx);
        }
        return allocation;
    }

    private static RunResult computeMetrics(List<Order> orders, Map<Integer, List<Proposal>> proposals, Map<Integer, Integer> allocation, long execTimeMs) {
        RunResult r = new RunResult();
        r.executionTimeMs = execTimeMs;
        int onTime = 0;
        int total = 0;
        List<Double> costs = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < orders.size(); i++) {
            Integer propIdx = allocation.get(i);
            if (propIdx == null) continue;
            List<Proposal> plist = proposals.get(i);
            if (plist == null || propIdx >= plist.size()) continue;
            Proposal p = plist.get(propIdx);
            Order order = orders.get(i);
            total++;
            r.totalCost += p.getPrice();
            costs.add(p.getPrice());
            if (order.getDeadline() != null && p.getEstimatedDelivery() != null) {
                if (p.getEstimatedDelivery().getTime() <= order.getDeadline().getTime()) {
                    onTime++;
                }
            }
        }
        r.serviceLevel = total > 0 ? (double) onTime / numOrders : 0;
        r.giniCoefficient = computeGini(costs);
        return r;
    }

    private static double computeGini(List<Double> values) {
        if (values == null || values.size() <= 1) return 0.0;
        Collections.sort(values);
        int n = values.size();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += values.get(i) * (i + 1);
        }
        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return 0;
        double gini = (2.0 * sum) / (n * total) - (n + 1.0) / n;
        return Math.max(0, gini);
    }

    private static Map<Integer, List<Proposal>> deepCopyProposals(
            Map<Integer, List<Proposal>> original) {
        Map<Integer, List<Proposal>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<Proposal>> e : original.entrySet()) {
            List<Proposal> origList = e.getValue();
            List<Proposal> copyList = new ArrayList<>(origList.size());
            for (Proposal p : origList) {
                Proposal cp = new Proposal();
                cp.setPrice(p.getPrice());
                cp.setReliability(p.getReliability());
                cp.setEstimatedDelivery(p.getEstimatedDelivery() != null
                        ? new Date(p.getEstimatedDelivery().getTime()) : null);
                cp.setOrder(p.getOrder());
                cp.setResource(p.getResource());
                copyList.add(cp);
            }
            copy.put(e.getKey(), copyList);
        }
        return copy;
    }

    private static class RunResult {
        double serviceLevel;
        double totalCost;
        long executionTimeMs;
        double giniCoefficient;
    }
}