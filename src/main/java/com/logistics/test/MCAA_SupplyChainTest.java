package com.logistics.test;

import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.test.data.SupplyChainDataLoader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MCAA_SupplyChainTest {
    private static final String[] ALGORITHMS = {"MCAA", "Vickrey", "DoubleAuction", "LP"};
    private static final String DATA_DIR = "data/Supply Chain Logistics Problem";
    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public static void main(String[] args) {
        int sampleSize = Integer.getInteger("sample.size", 0);
        long seed = Long.getLong("seed", 42L);
        String timestamp = TS_FMT.format(new Date());
        System.out.println("=== MCAA Supply Chain Logistics Test ===");
        System.out.printf("Sample: %s | Seed: %d%n", sampleSize > 0 ? String.valueOf(sampleSize) : "ALL", seed);
        System.out.println("=========================================\n");
        try {
            List<Order> orders = SupplyChainDataLoader.loadOrders(DATA_DIR, sampleSize);
            System.out.println(orders.size() + " orders loaded.");
            Map<Integer, List<Proposal>> proposals = SupplyChainDataLoader.loadProposals(orders, DATA_DIR);
            System.out.println("Proposals generated.\n");
            String csvPath = "mcaa_test_reports/" + timestamp + "_supplychain_results.csv";
            try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
                MCAA_OlistTest.writeCsvHeader(csv);
                for (String algo : ALGORITHMS) {
                    MCAA_OlistTest.RunResult r = MCAA_OlistTest.runAlgorithm(algo, orders, proposals, seed);
                    MCAA_OlistTest.writeCsvRow(csv, seed, orders.size(), algo, "supplychain", r);
                    System.out.printf("  %-14s  Svc=%.4f  Cost=%.0f  Time=%dms  Gini=%.4f%n", algo, r.serviceLevel, r.totalCost, r.executionTimeMs, r.giniCoefficient);
                }
            }
            System.out.println("\nResults: " + csvPath);
        } catch (Exception e) {
            System.err.println("MCAA_SupplyChainTest failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}