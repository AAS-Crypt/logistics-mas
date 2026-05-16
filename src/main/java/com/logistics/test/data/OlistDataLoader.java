package com.logistics.test.data;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OlistDataLoader {
    private static final String ORDERS_FILE = "olist_orders_dataset.csv";
    private static final String PAYMENTS_FILE = "olist_order_payments_dataset.csv";
    private static final String ITEMS_FILE = "olist_order_items_dataset.csv";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static List<Order> loadOrders(String dataDir, int maxOrders) throws IOException {
        Map<String, Double> paymentMap = loadPayments(dataDir);
        List<Order> orders = new ArrayList<>();
        File ordersFile = new File(dataDir, ORDERS_FILE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(ordersFile), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (maxOrders > 0 && orders.size() >= maxOrders) break;
                String[] cols = parseCsvLine(line);
                if (cols.length < 8) continue;
                String orderId = unquote(cols[0]);
                String status = unquote(cols[2]);
                if (!"delivered".equalsIgnoreCase(status)) continue;
                String estimatedDeliveryStr = unquote(cols[7]); 
                Date deadline;
                try {
                    deadline = DATE_FMT.parse(estimatedDeliveryStr);
                } catch (ParseException e) {
                    deadline = new Date(System.currentTimeMillis() + 30L * 24 * 3600_000);
                }
                Order o = new Order();
                o.setOrderId(orderId);
                o.setDeadline(deadline);
                double budget = paymentMap.getOrDefault(orderId, 500.0);
                budget *= 1.2;
                o.setMaxBudget(budget);
                o.setPriority(3); 
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
            System.out.println("  [Olist] Shifted " + orders.size() + " order deadlines forward by " + (offset / (24 * 3600_000L)) + " days (earliest deadline basis)");
        }
        if (!orders.isEmpty()) {
            List<Order> sorted = new ArrayList<>(orders);
            sorted.sort(Comparator.comparing(o -> o.getDeadline() != null ? o.getDeadline() : new Date(Long.MAX_VALUE)));
            int n = sorted.size();
            for (int i = 0; i < n; i++) {
                int priority = 5 - (int) ((double) i / n * 4); // 5 to 1
                sorted.get(i).setPriority(Math.max(1, Math.min(5, priority)));
            }
        }
        return orders;
    }
    public static Map<Integer, List<Proposal>> loadProposals(List<Order> orders, String dataDir) throws IOException {
        Map<String, List<String[]>> orderSellers = loadOrderSellers(dataDir);
        Map<String, Double> deliveryDurations = loadDeliveryDurations(dataDir);
        Map<Integer, List<Proposal>> proposals = new HashMap<>();
        Random rng = new Random(42);

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            String orderId = order.getOrderId();
            List<Proposal> plist = new ArrayList<>();
            List<String[]> sellers = orderSellers.get(orderId);
            if (sellers == null || sellers.isEmpty()) {
                Proposal p = new Proposal();
                p.setPrice(order.getMaxBudget() * 0.7);
                p.setReliability(0.85);
                long etaMs = System.currentTimeMillis()
                        + (long) ((14 + rng.nextDouble() * 21) * 24 * 3600_000L);
                p.setEstimatedDelivery(new Date(etaMs));
                plist.add(p);
            } else {
                double baseDuration = deliveryDurations.getOrDefault(orderId, 120.0); // hours
                for (String[] sellerInfo : sellers) {
                    Proposal p = new Proposal();
                    double price = Double.parseDouble(sellerInfo[1]);
                    p.setPrice(price * (0.85 + rng.nextDouble() * 0.3));
                    int sellerCount = countSellerOrders(sellers, sellerInfo[0]);
                    p.setReliability(0.7 + Math.min(0.3, sellerCount * 0.05));
                    double noiseHours = baseDuration * 0.2 * rng.nextGaussian();
                    double etaHours = Math.max(4, baseDuration + noiseHours);
                    long etaMs = System.currentTimeMillis() + (long) (etaHours * 3600_000L);
                    p.setEstimatedDelivery(new Date(etaMs));
                    plist.add(p);
                }
            }
            proposals.put(i, plist);
        }
        return proposals;
    }

    private static Map<String, Double> loadPayments(String dataDir) throws IOException {
        Map<String, Double> map = new HashMap<>();
        File f = new File(dataDir, PAYMENTS_FILE);
        if (!f.exists()) return map;

        try (BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = parseCsvLine(line);
                if (cols.length < 3) continue;
                String orderId = unquote(cols[0]);
                double value;
                try { value = Double.parseDouble(unquote(cols[2])); } catch (NumberFormatException e) { continue; }
                map.merge(orderId, value, Double::sum);
            }
        }
        return map;
    }

    private static Map<String, List<String[]>> loadOrderSellers(String dataDir) throws IOException {
        Map<String, List<String[]>> map = new HashMap<>();
        Map<String, Double> paymentMap = loadPayments(dataDir);
        File f = new File(dataDir, ITEMS_FILE);
        if (!f.exists()) return map;

        try (BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = parseCsvLine(line);
                if (cols.length < 5) continue;
                String orderId = unquote(cols[0]);
                String sellerId = unquote(cols[3]);
                double price;
                try { price = Double.parseDouble(unquote(cols[4])); } catch (NumberFormatException e) { price = 100; }
                map.computeIfAbsent(orderId, k -> new ArrayList<>()).add(new String[]{sellerId, String.valueOf(price)});
            }
        }
        return map;
    }

    private static Map<String, Double> loadDeliveryDurations(String dataDir) throws IOException {
        Map<String, Double> map = new HashMap<>();
        File f = new File(dataDir, ORDERS_FILE);
        if (!f.exists()) return map;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = parseCsvLine(line);
                if (cols.length < 8) continue;
                String orderId = unquote(cols[0]);
                String purchaseDateStr = unquote(cols[3]);
                String deliveredStr = unquote(cols[6]);
                if (purchaseDateStr.isEmpty() || deliveredStr.isEmpty()) continue;
                try {
                    Date purchase = DATE_FMT.parse(purchaseDateStr);
                    Date delivered = DATE_FMT.parse(deliveredStr);
                    double hours = (delivered.getTime() - purchase.getTime()) / 3600_000.0;
                    if (hours > 0) map.put(orderId, hours);
                } catch (ParseException ignored) {}
            }
        }
        return map;
    }

    private static int countSellerOrders(List<String[]> sellers, String sellerId) {
        int count = 0;
        for (String[] s : sellers) {
            if (s[0].equals(sellerId)) count++;
        }
        return count;
    }
    static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }
}