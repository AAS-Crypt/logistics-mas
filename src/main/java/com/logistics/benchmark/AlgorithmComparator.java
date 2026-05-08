package com.logistics.benchmark;

import com.logistics.algorithms.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.util.*;


public class AlgorithmComparator {

    private static final int NUM_ORDERS = 20;
    private static final int NUM_RESOURCES = 8;

    
    private static class AlgoResult {
        String name;
        double totalCost;
        double totalTimeHours;
        double avgReliability;
        long   execTimeMs;
        double score;            

        AlgoResult(String n) { this.name = n; }

        double combinedScore() {
            double costScore       = 1.0 / (1.0 + totalCost / 10000);
            double timeScore       = 1.0 / (1.0 + totalTimeHours / 100);
            double reliabilityScore = avgReliability;
            return costScore * 0.3 + timeScore * 0.4 + reliabilityScore * 0.3;
        }
    }

    public static void main(String[] args) {
        
        String algorithmsStr = "MCAA,GA,PSO,Random";
        String scenariosStr  = "uncertainty";
        int    iterations    = 20;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--algorithms":
                    if (i + 1 < args.length) algorithmsStr = args[++i];
                    break;
                case "--scenarios":
                    if (i + 1 < args.length) scenariosStr = args[++i];
                    break;
                case "--iterations":
                    if (i + 1 < args.length) {
                        try {
                            iterations = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("WARNING: invalid --iterations, using 20");
                            iterations = 20;
                        }
                    }
                    break;
                default:
                    System.err.println("WARNING: unknown flag: " + args[i]);
                    break;
            }
        }

        List<String> algorithms = splitAndTrim(algorithmsStr);
        List<String> scenarios  = splitAndTrim(scenariosStr);

        System.out.println("=== Algorithm Comparator ===");
        System.out.println("Algorithms : " + String.join(", ", algorithms));
        System.out.println("Scenarios  : " + String.join(", ", scenarios));
        System.out.println("Iterations : " + iterations);
        System.out.println("=============================");
        System.out.println();

        System.out.println("Algorithm Comparator – running with algorithms: "
                + algorithmsStr + ", scenarios: " + scenariosStr
                + ", iterations: " + iterations);
        System.out.println();

        
        for (String scenario : scenarios) {
            System.out.println("---- Scenario: " + scenario + " ----");
            System.out.println("(Orders: " + NUM_ORDERS + ", Resources: " + NUM_RESOURCES + ")");
            System.out.println();

            
            Map<String, List<Double>> algoScores = new LinkedHashMap<>();
            Map<String, List<Long>>   algoTimes  = new LinkedHashMap<>();
            for (String a : algorithms) {
                algoScores.put(a, new ArrayList<>());
                algoTimes.put(a, new ArrayList<>());
            }

            for (int iter = 0; iter < iterations; iter++) {
                
                List<Order>                     orders    = generateOrders(scenario);
                Map<Integer, List<Proposal>>    proposals = generateProposals(orders, scenario);

                for (String algo : algorithms) {
                    AlgoResult r = runAlgorithm(algo, orders, proposals);
                    algoScores.get(algo).add(r.combinedScore());
                    algoTimes.get(algo).add(r.execTimeMs);
                }
            }

            
            System.out.printf("%-16s %8s %8s %8s %8s %10s%n",
                    "Algorithm", "AvgScore", "Min", "Max", "StdDev", "AvgTimeMs");
            System.out.println("---------------------------------------------------------");

            for (String algo : algorithms) {
                List<Double> scores = algoScores.get(algo);
                List<Long>   times  = algoTimes.get(algo);

                double avg   = scores.stream().mapToDouble(d -> d).average().orElse(0);
                double min   = scores.stream().mapToDouble(d -> d).min().orElse(0);
                double max   = scores.stream().mapToDouble(d -> d).max().orElse(0);
                double var   = scores.stream().mapToDouble(s -> Math.pow(s - avg, 2)).average().orElse(0);
                double stdDev = Math.sqrt(var);
                double avgMs = times.stream().mapToLong(Long::longValue).average().orElse(0);

                System.out.printf("%-16s %8.4f %8.4f %8.4f %8.4f %10.1f%n",
                        algo, avg, min, max, stdDev, avgMs);
            }

            
            String winner = algoScores.entrySet().stream()
                    .max(Comparator.comparingDouble(e ->
                            e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)))
                    .map(Map.Entry::getKey).orElse("N/A");
            System.out.println();
            System.out.println("Winner for '" + scenario + "': " + winner);
            System.out.println();
        }

        System.out.println("Algorithm comparison finished successfully.");
        System.out.println("Comparison completed.");
    }

    
    
    
    private static List<Order> generateOrders(String scenario) {
        List<Order> orders = new ArrayList<>();
        Random rand = new Random(42); 

        for (int i = 0; i < NUM_ORDERS; i++) {
            Order o = new Order();
            o.setOrderId("ORD-" + i);
            o.setPriority(1 + rand.nextInt(5));
            o.setMaxBudget(2000 + rand.nextDouble() * 8000);
            
            long deadlineMs = System.currentTimeMillis() + (long)((24 + rand.nextDouble() * 96) * 3600_000L);
            o.setDeadline(new Date(deadlineMs));
            orders.add(o);
        }
        return orders;
    }

    private static Map<Integer, List<Proposal>> generateProposals(List<Order> orders, String scenario) {
        Map<Integer, List<Proposal>> map = new HashMap<>();
        Random rand = new Random(137); 

        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> list = new ArrayList<>();
            int num = 2 + rand.nextInt(4);                     
            for (int j = 0; j < num && j < NUM_RESOURCES; j++) {
                Proposal p = new Proposal();
                double price       = 500 + rand.nextDouble() * 1500;
                double reliability = 0.7 + rand.nextDouble() * 0.3;
                long   deliveryMs  = System.currentTimeMillis() + (long)((12 + rand.nextDouble() * 36) * 3600_000L);

                p.setPrice(price);
                p.setReliability(reliability);
                p.setEstimatedDelivery(new Date(deliveryMs));
                list.add(p);
            }
            map.put(i, list);
        }
        return map;
    }

    
    
    
    private static AlgoResult runAlgorithm(String algoName,
                                           List<Order> orders,
                                           Map<Integer, List<Proposal>> proposals) {
        switch (algoName.toUpperCase()) {
            case "MCAA":       return runMCAA(orders, proposals);
            case "GA":         return runGA(orders, proposals);
            case "PSO":        return runPSO(orders, proposals);
            case "SA":         return runSA(orders, proposals);
            case "ROUNDROBIN":
            case "ROUND_ROBIN":
            case "ROUND-ROBIN":return runRoundRobin(orders, proposals);
            case "RANDOM":     return runRandom(orders, proposals);
            case "FCFS":       return runFCFS(orders, proposals);
            default:
                System.err.println("Unknown algorithm: " + algoName + " – falling back to Random");
                return runRandom(orders, proposals);
        }
    }

    private static AlgoResult runMCAA(List<Order> orders,
                                      Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("MCAA");
        long t0 = System.currentTimeMillis();
        int count = 0;
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
            Proposal best = plist.get(bestIdx);
            r.totalCost += best.getPrice();
            long remainMs = best.getEstimatedDelivery().getTime() - System.currentTimeMillis();
            r.totalTimeHours += Math.max(0, remainMs / 3600_000.0);
            r.avgReliability += best.getReliability();
            count++;
        }
        if (count > 0) r.avgReliability /= count;
        r.execTimeMs = System.currentTimeMillis() - t0;
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runGA(List<Order> orders,
                                    Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("GA");
        long t0 = System.currentTimeMillis();
        Map<Integer, Integer> alloc = GeneticAlgorithm.optimize(orders, proposals);
        r.execTimeMs = System.currentTimeMillis() - t0;
        accumulateFromAlloc(r, orders, proposals, alloc);
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runPSO(List<Order> orders,
                                     Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("PSO");
        long t0 = System.currentTimeMillis();
        Map<Integer, Integer> alloc = ParticleSwarmOptimization.optimize(orders, proposals);
        r.execTimeMs = System.currentTimeMillis() - t0;
        accumulateFromAlloc(r, orders, proposals, alloc);
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runSA(List<Order> orders,
                                    Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("SA");
        long t0 = System.currentTimeMillis();
        Map<Integer, Integer> alloc = SimulatedAnnealing.optimize(orders, proposals);
        r.execTimeMs = System.currentTimeMillis() - t0;
        accumulateFromAlloc(r, orders, proposals, alloc);
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runRoundRobin(List<Order> orders,
                                            Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("RoundRobin");
        long t0 = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            int idx = RoundRobinAllocator.selectProposal(plist);
            if (idx >= 0 && idx < plist.size()) {
                Proposal p = plist.get(idx);
                r.totalCost += p.getPrice();
                long remainMs = p.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                r.totalTimeHours += Math.max(0, remainMs / 3600_000.0);
                r.avgReliability += p.getReliability();
                count++;
            }
        }
        if (count > 0) r.avgReliability /= count;
        r.execTimeMs = System.currentTimeMillis() - t0;
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runRandom(List<Order> orders,
                                        Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("Random");
        long t0 = System.currentTimeMillis();
        Random rand = new Random();
        int count = 0;
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            int idx = rand.nextInt(plist.size());
            Proposal p = plist.get(idx);
            r.totalCost += p.getPrice();
            long remainMs = p.getEstimatedDelivery().getTime() - System.currentTimeMillis();
            r.totalTimeHours += Math.max(0, remainMs / 3600_000.0);
            r.avgReliability += p.getReliability();
            count++;
        }
        if (count > 0) r.avgReliability /= count;
        r.execTimeMs = System.currentTimeMillis() - t0;
        r.score = r.combinedScore();
        return r;
    }

    private static AlgoResult runFCFS(List<Order> orders,
                                      Map<Integer, List<Proposal>> proposals) {
        AlgoResult r = new AlgoResult("FCFS");
        long t0 = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> plist = proposals.get(i);
            if (plist == null || plist.isEmpty()) continue;
            int idx = FCFSAllocator.selectProposal(plist);
            if (idx >= 0 && idx < plist.size()) {
                Proposal p = plist.get(idx);
                r.totalCost += p.getPrice();
                long remainMs = p.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                r.totalTimeHours += Math.max(0, remainMs / 3600_000.0);
                r.avgReliability += p.getReliability();
                count++;
            }
        }
        if (count > 0) r.avgReliability /= count;
        r.execTimeMs = System.currentTimeMillis() - t0;
        r.score = r.combinedScore();
        return r;
    }

    private static void accumulateFromAlloc(AlgoResult r,
                                            List<Order> orders,
                                            Map<Integer, List<Proposal>> proposals,
                                            Map<Integer, Integer> alloc) {
        int count = 0;
        for (Map.Entry<Integer, Integer> e : alloc.entrySet()) {
            List<Proposal> plist = proposals.get(e.getKey());
            if (plist != null && e.getValue() < plist.size()) {
                Proposal p = plist.get(e.getValue());
                r.totalCost += p.getPrice();
                long remainMs = p.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                r.totalTimeHours += Math.max(0, remainMs / 3600_000.0);
                r.avgReliability += p.getReliability();
                count++;
            }
        }
        if (count > 0) r.avgReliability /= count;
    }

    
    
    
    private static List<String> splitAndTrim(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) return result;
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }
}