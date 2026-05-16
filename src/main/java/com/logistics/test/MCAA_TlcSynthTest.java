package com.logistics.test;

import com.logistics.algorithms.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.test.data.TlcDataLoader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MCAA_TlcSynthTest {
    private static final String[] ALGORITHMS = {"MCAA", "Vickrey", "DoubleAuction", "LP"};
    private static final String DATA_DIR = "data/TLC Trip Record Data";
    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public static void main(String[] args) {
        int sampleSize = Integer.getInteger("sample.size", 5000);
        long seed = Long.getLong("seed", 42L);
        String timestamp = TS_FMT.format(new Date());
        System.out.println("=== MCAA TLC Synthetic Deadline Test ===");
        System.out.printf("Sample: %s | Seed: %d%n", sampleSize > 0 ? String.valueOf(sampleSize) : "ALL", seed);
        System.out.println("==========================================\n");
        try {
            System.out.print("Loading TLC orders... ");
            List<Order> orders = TlcDataLoader.loadOrders(DATA_DIR, sampleSize);
            System.out.println(orders.size() + " orders loaded from authentic TLC data.");
            shiftDeadlines(orders);
            System.out.printf("  Deadlines shifted: earliest is now %s%n", orders.get(0).getDeadline());
            System.out.print("Generating proposals... ");
            Map<Integer, List<Proposal>> proposals = TlcDataLoader.loadProposals(orders, DATA_DIR);
            System.out.println("done.\n");
            String csvPath = "mcaa_test_reports/" + timestamp + "_tlc_synth_results.csv";
            try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
                MCAA_OlistTest.writeCsvHeader(csv);
                for (String algo : ALGORITHMS) {
                    MCAA_OlistTest.RunResult r = MCAA_OlistTest.runAlgorithm(algo, orders, proposals, seed);
                    MCAA_OlistTest.writeCsvRow(csv, seed, orders.size(), algo, "tlc_synth", r);
                    System.out.printf("  %-14s  Svc=%.4f  Cost=%.0f  Time=%dms  Gini=%.4f%n",
                            algo, r.serviceLevel, r.totalCost, r.executionTimeMs, r.giniCoefficient);
                }
            }
            System.out.println("\nResults: " + csvPath);
        } catch (Exception e) {
            System.err.println("MCAA_TlcSynthTest failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void shiftDeadlines(List<Order> orders) {
        if (orders.isEmpty()) return;
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
        System.out.printf("  [TLC Synth] Shifted %d order deadlines forward by %d days (earliest deadline basis)%n", orders.size(), offset / (24 * 3600_000L));
    }
}