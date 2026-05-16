package com.logistics.test.data;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SupplyChainDataLoader {
    private static final String ORDERS_FILE = "order_list.csv";
    private static final String FREIGHT_FILE = "freight_rates.csv";
    private static final String WH_COSTS_FILE = "wh_costs.csv";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM.yyyy");
    public static List<Order> loadOrders(String dataDir, int maxOrders) throws IOException {
        List<Order> orders = new ArrayList<>();
        File f = new File(dataDir, ORDERS_FILE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (maxOrders > 0 && orders.size() >= maxOrders) break;
                String[] cols = line.split(";", -1);
                if (cols.length < 14) continue;

                String orderId = cols[0].trim();
                String dateStr = cols[1].trim();
                String serviceLevel = cols[4].trim();
                int shipAhead = parseIntSafe(cols[6]);
                int shipLate = parseIntSafe(cols[7]);
                double weight = parseDoubleSafe(cols[13]);
                Order o = new Order();
                o.setOrderId(orderId);
                Date deadline;
                try {
                    Date orderDate = DATE_FMT.parse(dateStr);
                    long deadlineMs = orderDate.getTime() + (long) shipAhead * 24 * 3600_000L;
                    deadline = new Date(deadlineMs);
                } catch (ParseException e) {
                    deadline = new Date(System.currentTimeMillis() + 30L * 24 * 3600_000);
                }
                o.setDeadline(deadline);
                double budget = weight * (5 + Math.random() * 10);
                o.setMaxBudget(budget);
                int priority = serviceLevel.startsWith("DTD") ? 5 : serviceLevel.startsWith("DTP") ? 3 : 2;
                if (shipLate > 0) priority = Math.min(5, priority + 1);
                o.setPriority(priority);
                orders.add(o);
            }
        }

        if (!orders.isEmpty()) {
            long now = System.currentTimeMillis();
            long earliestDeadline = Long.MAX_VALUE;
            for (Order o : orders) {
                if (o.getDeadline() != null) {
                    earliestDeadline = Math.min(earliestDeadline, o.getDeadline().getTime());
                }
            }
            long offset = now - earliestDeadline + 7L * 24 * 3600_000L;
            for (Order o : orders) {
                if (o.getDeadline() != null) {
                    o.setDeadline(new Date(o.getDeadline().getTime() + offset));
                }
            }
            System.out.println("  [SupplyChain] Shifted " + orders.size() + " order deadlines forward by " + (offset / (24 * 3600_000L)) + " days (earliest deadline basis)");
        }
        return orders;
    }

    public static Map<Integer, List<Proposal>> loadProposals(List<Order> orders, String dataDir) throws IOException {
        Map<String, List<RateBracket>> carrierRates = loadFreightRates(dataDir);
        Map<String, Double> whCosts = loadWarehouseCosts(dataDir);
        Map<Integer, List<Proposal>> proposals = new HashMap<>();
        Random rng = new Random(137);
        Map<String, Double> orderWeights = loadOrderWeights(dataDir);
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            double weight = orderWeights.getOrDefault(order.getOrderId(), 10.0);
            List<Proposal> plist = new ArrayList<>();
            int carIdx = 0;
            for (Map.Entry<String, List<RateBracket>> entry : carrierRates.entrySet()) {
                if (carIdx++ >= 6) break;
                String carrier = entry.getKey();
                List<RateBracket> brackets = entry.getValue();
                RateBracket best = null;
                for (RateBracket rb : brackets) {
                    if (weight >= rb.minWeight && weight <= rb.maxWeight) {
                        best = rb;
                        break;
                    }
                }
                if (best == null) best = brackets.get(brackets.size() - 1);
                Proposal p = new Proposal();
                double freightCost = Math.max(best.minCost, best.rate * weight);
                double whCost = whCosts.values().stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
                p.setPrice(freightCost + whCost);
                double reliability = best.mode.contains("AIR") ? 0.95 : 0.80;
                p.setReliability(reliability + rng.nextDouble() * 0.05);
                double baseHours = best.tptDays * 24;
                double noise = baseHours * 0.15 * rng.nextGaussian();
                double etaHours = Math.max(4, baseHours + noise);
                long etaMs = System.currentTimeMillis() + (long) (etaHours * 3600_000L);
                p.setEstimatedDelivery(new Date(etaMs));
                plist.add(p);
            }
            if (plist.isEmpty()) {
                Proposal fallback = new Proposal();
                fallback.setPrice(order.getMaxBudget() * 0.8);
                fallback.setReliability(0.80);
                fallback.setEstimatedDelivery(new Date(System.currentTimeMillis() + 14L * 24 * 3600_000L));
                plist.add(fallback);
            }
            proposals.put(i, plist);
        }
        return proposals;
    }
    private static Map<String, List<RateBracket>> loadFreightRates(String dataDir) throws IOException {
        Map<String, List<RateBracket>> map = new HashMap<>();
        File f = new File(dataDir, FREIGHT_FILE);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(";", -1);
                if (cols.length < 11) continue;
                String carrier = cols[0].trim();
                double minWeight = parseDoubleSafe(cols[3]);
                double maxWeight = parseDoubleSafe(cols[4]);
                String svcCd = cols[5].trim();
                double minCost = parseDoubleSafe(cols[6]);
                double rate = parseDoubleSafe(cols[7]);
                String mode = cols[8].trim();
                int tptDays = parseIntSafe(cols[9]);
                map.computeIfAbsent(carrier, k -> new ArrayList<>())
                   .add(new RateBracket(minWeight, maxWeight, minCost, rate, mode, tptDays));
            }
        }
        return map;
    }

    private static Map<String, Double> loadWarehouseCosts(String dataDir) throws IOException {
        Map<String, Double> map = new HashMap<>();
        File f = new File(dataDir, WH_COSTS_FILE);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;
                map.put(cols[0].trim(), parseDoubleSafe(cols[1]));
            }
        }
        return map;
    }

    private static Map<String, Double> loadOrderWeights(String dataDir) throws IOException {
        Map<String, Double> map = new HashMap<>();
        File f = new File(dataDir, ORDERS_FILE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(";", -1);
                if (cols.length < 14) continue;
                map.put(cols[0].trim(), parseDoubleSafe(cols[13]));
            }
        }
        return map;
    }

    static int parseIntSafe(String s) {
        s = s.trim();
        if (s.isEmpty()) return 0;
        try { return (int) Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    static double parseDoubleSafe(String s) {
        s = s.trim().replace(",", ".");
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private static class RateBracket {
        final double minWeight, maxWeight, minCost, rate;
        final String mode;
        final int tptDays;

        RateBracket(double minWeight, double maxWeight, double minCost, double rate, String mode, int tptDays) {
            this.minWeight = minWeight;
            this.maxWeight = maxWeight;
            this.minCost = minCost;
            this.rate = rate;
            this.mode = mode;
            this.tptDays = tptDays;
        }
    }
}