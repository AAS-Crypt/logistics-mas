package com.logistics.test.data;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class IncomDataLoader {
    private static final String DELAY_FILE = "incom2024_delay_example_dataset.csv";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static List<Order> loadOrders(String dataDir, int maxOrders) throws IOException {
        List<Order> orders = new ArrayList<>();
        File f = new File(dataDir, DELAY_FILE);
        if (!f.exists()) {
            System.err.println("WARNING: " + f.getAbsolutePath() + " not found, using synthetic data");
            return generateSyntheticOrders(maxOrders);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {
            String header = br.readLine();
            Map<String, Integer> colIndex = mapColumns(header);

            String line;
            while ((line = br.readLine()) != null) {
                if (maxOrders > 0 && orders.size() >= maxOrders) break;
                String[] cols = OlistDataLoader.parseCsvLine(line);
                String orderDateStr = getCol(cols, colIndex, "order_date");
                String shippingStr = getCol(cols, colIndex, "shipping_date");
                Date deadline = estimateDeadline(orderDateStr, shippingStr);
                Order o = new Order();
                String orderId = getCol(cols, colIndex, "order_id");
                if (orderId == null || orderId.isEmpty())
                    orderId = "INC-" + orders.size();
                o.setOrderId(orderId);
                o.setDeadline(deadline);
                double profit = parseDoubleSafe(getCol(cols, colIndex, "profit_per_order"));
                double sales = parseDoubleSafe(getCol(cols, colIndex, "sales"));
                double budget = profit > 0 ? profit * 1.5 : (sales > 0 ? sales * 0.3 : 1000);
                o.setMaxBudget(budget);
                String status = getCol(cols, colIndex, "order_status");
                int priority = 3;
                if (status != null) {
                    status = status.toUpperCase();
                    if (status.contains("CANCEL") || status.contains("FAIL")) priority = 5;
                    else if (status.contains("COMPLETE")) priority = 2;
                    else if (status.contains("PENDING")) priority = 4;
                }
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
            if (earliestDeadline < now) {
                long offset = now - earliestDeadline + 7L * 24 * 3600_000L;
                for (Order o : orders) {
                    if (o.getDeadline() != null) {
                        o.setDeadline(new Date(o.getDeadline().getTime() + offset));
                    }
                }
                System.out.println("  [INCOM] Shifted " + orders.size() + " order deadlines forward by " + (offset / (24 * 3600_000L)) + " days (earliest deadline basis)");
            }
        }
        return orders.isEmpty() ? generateSyntheticOrders(maxOrders) : orders;
    }

    public static Map<Integer, List<Proposal>> loadProposals(List<Order> orders, String dataDir) throws IOException {
        Map<Integer, List<Proposal>> proposals = new HashMap<>();
        Random rng = new Random(42);
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            List<Proposal> plist = new ArrayList<>();
            int numProviders = 2 + rng.nextInt(3);
            for (int j = 0; j < numProviders; j++) {
                Proposal p = new Proposal();
                p.setPrice(order.getMaxBudget() * (0.4 + rng.nextDouble() * 0.4));
                double baseReliability = 0.75 + j * 0.08;
                p.setReliability(baseReliability + rng.nextDouble() * 0.05);
                double baseHours = (3 + rng.nextDouble() * 11) * 24;
                double noise = baseHours * 0.3 * rng.nextGaussian();
                double etaHours = Math.max(8, baseHours + noise);
                long etaMs = System.currentTimeMillis() + (long) (etaHours * 3600_000L);
                p.setEstimatedDelivery(new Date(etaMs));
                plist.add(p);
            }
            proposals.put(i, plist);
        }
        return proposals;
    }

    private static Map<String, Integer> mapColumns(String header) {
        Map<String, Integer> map = new HashMap<>();
        String[] cols = header.split(",");
        for (int i = 0; i < cols.length; i++) {
            map.put(OlistDataLoader.unquote(cols[i]).trim(), i);
        }
        return map;
    }

    private static String getCol(String[] cols, Map<String, Integer> map, String name) {
        Integer idx = map.get(name);
        if (idx == null || idx >= cols.length) return null;
        return OlistDataLoader.unquote(cols[idx]);
    }

    private static Date estimateDeadline(String orderDateStr, String shippingStr) {
        Date ref;
        if (orderDateStr != null && !orderDateStr.isEmpty()) {
            try { ref = DATE_FMT.parse(orderDateStr); }
            catch (ParseException e) { ref = new Date(); }
        } else if (shippingStr != null && !shippingStr.isEmpty()) {
            try { ref = DATE_FMT.parse(shippingStr); }
            catch (ParseException e) { ref = new Date(); }
        } else {
            ref = new Date();
        }
        long slaMs = (long) ((7 + Math.random() * 7) * 24 * 3600_000L);
        return new Date(ref.getTime() + slaMs);
    }

    private static double parseDoubleSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static List<Order> generateSyntheticOrders(int maxOrders) {
        List<Order> orders = new ArrayList<>();
        int n = maxOrders > 0 ? maxOrders : 50;
        Random rng = new Random(42);
        long now = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Order o = new Order();
            o.setOrderId("SYN-INC-" + i);
            o.setMaxBudget(500 + rng.nextDouble() * 4500);
            o.setPriority(1 + rng.nextInt(5));
            o.setDeadline(new Date(now + (long) ((7 + rng.nextDouble() * 21) * 24 * 3600_000L)));
            orders.add(o);
        }
        return orders;
    }
}