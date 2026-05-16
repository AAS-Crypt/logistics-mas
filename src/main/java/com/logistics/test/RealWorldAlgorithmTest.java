package com.logistics.test;

import com.logistics.algorithms.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.concepts.Location;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.test.data.RealWorldTestData;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class RealWorldAlgorithmTest {
    private static class RealWorldScenario {
        String name;
        String description;
        List<String> warehouses;
        List<String> customers;
        double budgetMultiplier;
        double timeMultiplier;
        int numOrders;
        RealWorldScenario(String name, String description, List<String> warehouses, List<String> customers, double budgetMultiplier, double timeMultiplier, int numOrders) {
            this.name = name;
            this.description = description;
            this.warehouses = warehouses;
            this.customers = customers;
            this.budgetMultiplier = budgetMultiplier;
            this.timeMultiplier = timeMultiplier;
            this.numOrders = numOrders;
        }
    }
    private static class AlgorithmResult {
        String algorithmName;
        Map<Integer, Integer> allocation;
        double totalCost;
        double totalDistance; 
        double totalTime; 
        double avgReliability;
        double executionTime; 
        double score;
        Map<String, Double> metrics; 
        AlgorithmResult(String name) {
            this.algorithmName = name;
            this.allocation = new HashMap<>();
            this.metrics = new HashMap<>();
        }
    }
    public static void main(String[] args) {
        System.out.println("=== Real-World Logistics Algorithm Comparison Test ===\n");
        List<RealWorldScenario> scenarios = createRealWorldScenarios();
        Map<String, Map<String, AlgorithmResult>> allResults = new HashMap<>();
        for (RealWorldScenario scenario : scenarios) {
            System.out.println("Testing scenario: " + scenario.name);
            System.out.println("Description: " + scenario.description);
            System.out.println("Warehouses: " + scenario.warehouses);
            System.out.println("Customers: " + scenario.customers);
            System.out.println("Orders: " + scenario.numOrders);
            Map<String, AlgorithmResult> scenarioResults = runRealWorldScenario(scenario);
            allResults.put(scenario.name, scenarioResults);
            printScenarioSummary(scenario, scenarioResults);
            System.out.println();
        }
        generateReport(allResults, scenarios);
        System.out.println("Test completed. Report generated.");
    }
    private static List<RealWorldScenario> createRealWorldScenarios() {
        List<RealWorldScenario> scenarios = new ArrayList<>();
        scenarios.add(new RealWorldScenario(
            "Cross-Country Delivery",
            "Deliveries from LA warehouse to East Coast customers",
            Arrays.asList("Los Angeles"),
            Arrays.asList("New York", "Boston", "Philadelphia", "Charlotte", "Jacksonville"),
            1.0, 1.0, 10
        ));

        scenarios.add(new RealWorldScenario(
            "Midwest Regional Hub",
            "Chicago warehouse serving Midwest region",
            Arrays.asList("Chicago"),
            Arrays.asList("Columbus", "Nashville", "Denver", "Dallas", "Houston"),
            0.8, 1.2, 8
        ));
        
        scenarios.add(new RealWorldScenario(
            "Multi-Warehouse Network",
            "Multiple warehouses serving nationwide customers",
            Arrays.asList("Chicago", "Los Angeles", "Dallas", "Atlanta"),
            Arrays.asList("New York", "Seattle", "Phoenix", "Boston", "San Diego"),
            1.0, 1.0, 12
        ));
        
        scenarios.add(new RealWorldScenario(
            "Time-Critical Express",
            "Urgent deliveries with tight time constraints",
            Arrays.asList("Dallas"),
            Arrays.asList("Houston", "Austin", "San Antonio", "Fort Worth"),
            1.5, 0.5, 6
        ));
        
        scenarios.add(new RealWorldScenario(
            "Budget-Constrained Nationwide",
            "Cost-optimized deliveries across the country",
            Arrays.asList("Atlanta", "Chicago"),
            Arrays.asList("New York", "Los Angeles", "Seattle", "Miami", "Denver"),
            0.6, 1.5, 10
        ));
        return scenarios;
    }
    private static Map<String, AlgorithmResult> runRealWorldScenario(RealWorldScenario scenario) {
        Map<String, AlgorithmResult> results = new HashMap<>();
        List<Order> orders = generateRealWorldOrders(scenario);
        Map<Integer, List<Proposal>> proposals = generateRealWorldProposals(orders, scenario);
        AlgorithmResult mcaaResult = runMCAA(orders, proposals, scenario);
        results.put("MCAA", mcaaResult);
        AlgorithmResult gaResult = runGeneticAlgorithm(orders, proposals, scenario);
        results.put("GA", gaResult);
        AlgorithmResult saResult = runSimulatedAnnealing(orders, proposals, scenario);
        results.put("SA", saResult);
        AlgorithmResult psoResult = runPSO(orders, proposals, scenario);
        results.put("PSO", psoResult);
        AlgorithmResult hybridMCAA_GA = runHybridMCAA_GA(orders, proposals, scenario);
        results.put("MCAA+GA", hybridMCAA_GA);
        AlgorithmResult hybridMCAA_SA = runHybridMCAA_SA(orders, proposals, scenario);
        results.put("MCAA+SA", hybridMCAA_SA);
        AlgorithmResult hybridMCAA_PSO = runHybridMCAA_PSO(orders, proposals, scenario);
        results.put("MCAA+PSO", hybridMCAA_PSO);
        AlgorithmResult hybridAdaptive = runHybridAdaptive(orders, proposals, scenario);
        results.put("Adaptive", hybridAdaptive);
        results.put("RoundRobin", runRoundRobin(orders, proposals, scenario));
        results.put("Random", runRandom(orders, proposals, scenario));
        results.put("FCFS", runFCFS(orders, proposals, scenario));
        return results;
    }

    private static List<Order> generateRealWorldOrders(RealWorldScenario scenario) {
        List<Order> orders = new ArrayList<>();
        Random rand = new Random(42); 
        for (int i = 0; i < scenario.numOrders; i++) {
            Order order = new Order();
            orders.add(order);
        }
        return orders;
    }

    private static Map<Integer, List<Proposal>> generateRealWorldProposals(
            List<Order> orders, RealWorldScenario scenario) {
        Map<Integer, List<Proposal>> proposals = new HashMap<>();
        Random rand = new Random(42);
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = new ArrayList<>();
            String customerLocation = scenario.customers.get(i % scenario.customers.size());
            for (String warehouse : scenario.warehouses) {
                double distance = RealWorldTestData.getDistance(warehouse, customerLocation);
                double baseCost = 50 + (distance * 0.5); 
                double cost = baseCost * scenario.budgetMultiplier * (0.8 + rand.nextDouble() * 0.4);
                double baseTime = distance / 60.0; 
                double time = baseTime * scenario.timeMultiplier * (0.8 + rand.nextDouble() * 0.4);
                double reliability = Math.max(0.7, 1.0 - (distance / 10000.0));
                Proposal proposal = new Proposal();
                proposal.setPrice(cost);
                proposal.setReliability(reliability);
                long deliveryTime = System.currentTimeMillis() + (long)(time * 3600000);
                proposal.setEstimatedDelivery(new Date(deliveryTime));
                orderProposals.add(proposal);
            }
            proposals.put(i, orderProposals);
        }
        return proposals;
    }

    private static AlgorithmResult runMCAA(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("MCAA");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                int bestIdx = 0;
                double bestScore = -1;
                Order order = orders.get(i);
                for (int j = 0; j < orderProposals.size(); j++) {
                    double score = MCAA.computeScore(order, orderProposals.get(j));
                    if (score > bestScore) {
                        bestScore = score;
                        bestIdx = j;
                    }
                }
                result.allocation.put(i, bestIdx);
                Proposal bestProposal = orderProposals.get(bestIdx);
                result.totalCost += bestProposal.getPrice();
                long deliveryTimeMs = bestProposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += bestProposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runGeneticAlgorithm(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("GA");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = GeneticAlgorithm.optimize(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runSimulatedAnnealing(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("SA");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = SimulatedAnnealing.optimize(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runPSO(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("PSO");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = ParticleSwarmOptimization.optimize(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runRoundRobin(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("RoundRobin");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                int selected = RoundRobinAllocator.selectProposal(orderProposals);
                if (selected >= 0 && selected < orderProposals.size()) {
                    result.allocation.put(i, selected);
                    Proposal proposal = orderProposals.get(selected);
                    result.totalCost += proposal.getPrice();
                    long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                    double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                    result.totalTime += deliveryTimeHours;
                    result.avgReliability += proposal.getReliability();
                }
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runRandom(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("Random");
        long startTime = System.currentTimeMillis();
        Random rand = new Random(42);
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                int idx = rand.nextInt(orderProposals.size());
                result.allocation.put(i, idx);
                Proposal proposal = orderProposals.get(idx);
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runFCFS(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("FCFS");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < orders.size(); i++) {
            List<Proposal> orderProposals = proposals.get(i);
            if (orderProposals != null && !orderProposals.isEmpty()) {
                int selected = FCFSAllocator.selectProposal(orderProposals);
                if (selected >= 0 && selected < orderProposals.size()) {
                    result.allocation.put(i, selected);
                    Proposal proposal = orderProposals.get(selected);
                    result.totalCost += proposal.getPrice();
                    long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                    double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                    result.totalTime += deliveryTimeHours;
                    result.avgReliability += proposal.getReliability();
                }
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runHybridMCAA_GA(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("MCAA+GA");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = HybridMCAA.optimizeMCAA_GA(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runHybridMCAA_SA(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("MCAA+SA");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = HybridMCAA.optimizeMCAA_SA(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runHybridMCAA_PSO(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("MCAA+PSO");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = HybridMCAA.optimizeMCAA_PSO(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static AlgorithmResult runHybridAdaptive(List<Order> orders, Map<Integer, List<Proposal>> proposals, RealWorldScenario scenario) {
        AlgorithmResult result = new AlgorithmResult("Adaptive");
        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> allocation = HybridMCAA.optimizeAdaptive(orders, proposals);
        result.allocation = allocation;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<Proposal> orderProposals = proposals.get(entry.getKey());
            if (orderProposals != null && entry.getValue() < orderProposals.size()) {
                Proposal proposal = orderProposals.get(entry.getValue());
                result.totalCost += proposal.getPrice();
                long deliveryTimeMs = proposal.getEstimatedDelivery().getTime() - System.currentTimeMillis();
                double deliveryTimeHours = Math.max(0, deliveryTimeMs / 3600000.0);
                result.totalTime += deliveryTimeHours;
                result.avgReliability += proposal.getReliability();
            }
        }
        result.avgReliability /= orders.size();
        result.executionTime = System.currentTimeMillis() - startTime;
        result.score = calculateScore(result);
        return result;
    }

    private static double calculateScore(AlgorithmResult result) {
        double costScore = 1.0 / (1.0 + result.totalCost / 10000);
        double timeScore = 1.0 / (1.0 + result.totalTime / 100);
        double reliabilityScore = result.avgReliability;
        return costScore * 0.3 + timeScore * 0.4 + reliabilityScore * 0.3;
    }

    private static void printScenarioSummary(RealWorldScenario scenario, Map<String, AlgorithmResult> results) {
        System.out.println("Results for: " + scenario.name);
        System.out.println("Algorithm      | Score  | Cost     | Time    | Reliability | Exec Time");
        System.out.println("---------------|--------|----------|---------|-------------|----------");
        List<Map.Entry<String, AlgorithmResult>> sorted = new ArrayList<>(results.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue().score, a.getValue().score));
        for (Map.Entry<String, AlgorithmResult> entry : sorted) {
            AlgorithmResult r = entry.getValue();
            System.out.printf("%-14s | %.4f | $%,.0f | %,.1fh | %.3f       | %,dms\n",
                r.algorithmName, r.score, r.totalCost, r.totalTime, 
                r.avgReliability, (long) r.executionTime);
        }
        Map.Entry<String, AlgorithmResult> winner = sorted.get(0);
        System.out.println("Winner: " + winner.getKey() + " (score: " + String.format("%.4f", winner.getValue().score) + ")");
    }

    private static void generateReport(Map<String, Map<String, AlgorithmResult>> allResults, List<RealWorldScenario> scenarios) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "overall_test_reports\\real_world_algorithm_report_" + timestamp + ".html";
        try {
            String htmlContent = generateHTMLReport(allResults, scenarios);
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.print(htmlContent);
            }
            System.out.println("Report generated: " + filename);
        } catch (IOException e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }

    private static String generateHTMLReport(Map<String, Map<String, AlgorithmResult>> allResults, List<RealWorldScenario> scenarios) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <title>Real-World Algorithm Comparison Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }\n");
        html.append("    h2 { color: #34495e; margin-top: 30px; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("    th, td { border: 1px solid #bdc3c7; padding: 12px; text-align: left; }\n");
        html.append("    th { background-color: #3498db; color: white; }\n");
        html.append("    tr:nth-child(even) { background-color: #ecf0f1; }\n");
        html.append("    .winner { background-color: #d5f4e6; font-weight: bold; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>Real-World Logistics Algorithm Comparison Report</h1>\n");
        html.append("  <p>Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p>\n");
        for (RealWorldScenario scenario : scenarios) {
            html.append("  <h2>").append(scenario.name).append("</h2>\n");
            html.append("  <p>").append(scenario.description).append("</p>\n");
            html.append("  <p><strong>Warehouses:</strong> ").append(String.join(", ", scenario.warehouses)).append("</p>\n");
            html.append("  <p><strong>Customers:</strong> ").append(String.join(", ", scenario.customers)).append("</p>\n");
            Map<String, AlgorithmResult> results = allResults.get(scenario.name);
            if (results != null) {
                html.append("  <table>\n");
                html.append("    <tr><th>Algorithm</th><th>Score</th><th>Total Cost</th><th>Total Time</th><th>Reliability</th><th>Exec Time (ms)</th></tr>\n");
                List<Map.Entry<String, AlgorithmResult>> sorted = new ArrayList<>(results.entrySet());
                sorted.sort((a, b) -> Double.compare(b.getValue().score, a.getValue().score));
                for (int i = 0; i < sorted.size(); i++) {
                    Map.Entry<String, AlgorithmResult> entry = sorted.get(i);
                    AlgorithmResult r = entry.getValue();
                    String rowClass = (i == 0) ? " class=\"winner\"" : "";
                    html.append("    <tr").append(rowClass).append(">");
                    html.append("<td>").append(r.algorithmName).append("</td>");
                    html.append("<td>").append(String.format("%.4f", r.score)).append("</td>");
                    html.append("<td>$").append(String.format("%,.0f", r.totalCost)).append("</td>");
                    html.append("<td>").append(String.format("%,.1f", r.totalTime)).append("h</td>");
                    html.append("<td>").append(String.format("%.3f", r.avgReliability)).append("</td>");
                    html.append("<td>").append(String.format("%,d", (long) r.executionTime)).append("</td>");
                    html.append("</tr>\n");
                }
                html.append("  </table>\n");
            }
        }
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }
}