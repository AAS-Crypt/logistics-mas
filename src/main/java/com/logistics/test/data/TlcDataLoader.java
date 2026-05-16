package com.logistics.test.data;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TlcDataLoader {
    private static final String CSV_PATH = "data/TLC Trip Record Data/tlc_all_trips.csv";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static List<Order> loadOrders(String dataDir, int maxOrders) throws IOException {
        File csvFile = new File(CSV_PATH);
        if (!csvFile.exists()) {
            System.err.println("WARNING: TLC CSV not found. Run scripts/convert_tlc_parquet_to_csv.py first.");
            System.err.println("WARNING: Falling back to synthetic data");
            return generateSyntheticOrders(maxOrders > 0 ? maxOrders : 2000);
        }
        List<Order> orders = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            if (header == null) throw new IOException("Empty CSV");
            String[] cols = header.split(",");
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) colIdx.put(cols[i].trim(), i);

            Integer pickupIdx = colIdx.get("pickup_datetime");
            Integer dropoffIdx = colIdx.get("dropoff_datetime");
            Integer totalIdx = colIdx.get("total_amount");
            Integer fareIdx = colIdx.get("fare_amount");
            Integer distIdx = colIdx.get("trip_distance");
            Integer vendorIdx = colIdx.get("vendor_id");

            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                if (maxOrders > 0 && orders.size() >= maxOrders) break;
                lineNum++;
                if (lineNum % 100000 == 0) {
                    System.out.printf("  TLC CSV: %d lines, %d orders loaded%n", lineNum, orders.size());
                }
                String[] vals = line.split(",", -1);
                if (vals.length < cols.length) continue;

                try {
                    Order o = new Order();
                    String vendor = vendorIdx != null ? vals[vendorIdx] : "TLC";
                    o.setOrderId(vendor + "-" + lineNum);
                    Date pickup = pickupIdx != null ? parseDate(vals[pickupIdx]) : new Date();
                    if (pickup == null) pickup = new Date();
                    Date dropoff = dropoffIdx != null ? parseDate(vals[dropoffIdx]) : new Date(pickup.getTime() + 30 * 60_000L);
                    if (dropoff == null) dropoff = new Date(pickup.getTime() + 30 * 60_000L);
                    o.setDeadline(dropoff);

                    double budget = 25.0;
                    if (totalIdx != null) {
                        try { budget = Double.parseDouble(vals[totalIdx]); } catch (Exception ignored) {}
                    } else if (fareIdx != null) {
                        try { budget = Double.parseDouble(vals[fareIdx]) * 1.5; } catch (Exception ignored) {}
                    }
                    o.setMaxBudget(Math.max(5, budget));

                    int priority = 3;
                    if (distIdx != null) {
                        try {
                            double dist = Double.parseDouble(vals[distIdx]);
                            if (dist > 20) priority = 5;
                            else if (dist > 10) priority = 4;
                            else if (dist > 3) priority = 3;
                            else priority = 2;
                        } catch (Exception ignored) {}
                    }
                    o.setPriority(priority);
                    orders.add(o);
                } catch (Exception ignored) {}
            }
        }
        System.out.println("Loaded " + orders.size() + " trips from TLC CSV");
        return orders;
    }

    private static Date parseDate(String s) {
        try { return DATE_FMT.parse(s.trim()); } catch (ParseException e) { return null; }
    }

    public static Map<Integer, List<Proposal>> loadProposals(List<Order> orders, String dataDir) {
        Map<Integer, List<Proposal>> proposals = new HashMap<>();
        Random rng = new Random(42);
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            List<Proposal> plist = new ArrayList<>();
            int numDrivers = 2 + rng.nextInt(4);
            for (int j = 0; j < numDrivers; j++) {
                Proposal p = new Proposal();
                p.setPrice(Math.max(5, order.getMaxBudget() * (0.6 + rng.nextDouble() * 0.35)));
                p.setReliability(0.75 + rng.nextDouble() * 0.24);
                double etaMin = Math.max(2, 5 + rng.nextDouble() * 45
                        + (5 + rng.nextDouble() * 45) * 0.25 * rng.nextGaussian());
                p.setEstimatedDelivery(new Date(System.currentTimeMillis() + (long) (etaMin * 60_000L)));
                plist.add(p);
            }
            proposals.put(i, plist);
        }
        return proposals;
    }

    private static List<Order> generateSyntheticOrders(int count) {
        List<Order> orders = new ArrayList<>();
        Random rng = new Random(42);
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Order o = new Order();
            o.setOrderId("SYN-TLC-" + i);
            o.setMaxBudget(5 + rng.nextDouble() * 95);
            o.setPriority(1 + rng.nextInt(5));
            o.setDeadline(new Date(now + (long) ((5 + rng.nextDouble() * 55) * 60_000L)));
            orders.add(o);
        }
        return orders;
    }
}