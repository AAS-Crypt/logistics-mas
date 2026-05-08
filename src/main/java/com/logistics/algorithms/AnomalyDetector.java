package com.logistics.algorithms;

import java.util.*;

public class AnomalyDetector {
    
    private double mean;
    private double stdDev;
    private List<Double> history;
    private int windowSize;
    private double threshold; 

    public AnomalyDetector(int windowSize, double threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        this.history = new ArrayList<>();
        this.mean = 0;
        this.stdDev = 1;
    }

    public void update(double value) {
        history.add(value);
        if (history.size() > windowSize) {
            history.remove(0);
        }
        calculateStatistics();
    }

    private void calculateStatistics() {
        if (history.isEmpty()) return;
        double sum = 0;
        for (double v : history) {
            sum += v;
        }
        mean = sum / history.size();
        
        double sumSquaredDiff = 0;
        for (double v : history) {
            sumSquaredDiff += Math.pow(v - mean, 2);
        }
        stdDev = Math.sqrt(sumSquaredDiff / history.size());
        
        if (stdDev < 0.001) {
            stdDev = 0.001;
        }
    }

    public boolean isAnomaly(double value) {
        if (history.size() < 5) {
            return false; 
        }
        
        double zScore = Math.abs(value - mean) / stdDev;
        return zScore > threshold;
    }

    public double getAnomalyScore(double value) {
        if (history.isEmpty()) return 0;
        
        double zScore = Math.abs(value - mean) / stdDev;
        return zScore;
    }

    public List<Integer> detectAnomalies(double[] timeSeries) {
        List<Integer> anomalyIndices = new ArrayList<>();
        for (int i = 0; i < timeSeries.length; i++) {
            update(timeSeries[i]);
            if (isAnomaly(timeSeries[i])) {
                anomalyIndices.add(i);
            }
        }
        
        return anomalyIndices;
    }

    public List<Double> filterEvents(List<Double> events) {
        List<Double> filtered = new ArrayList<>();
        for (Double event : events) {
            update(event);
            if (!isAnomaly(event)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    public static double calculateImpact(double severity, double distance, double confidence) {
        double fSeverity = Math.pow(severity, 1.5); 
        double gDistance = 1.0 / (1.0 + distance / 100.0); 
        return fSeverity * gDistance * confidence;
    }

    public static String determineReactionPath(double impact, double thetaLow, double thetaHigh) {
        if (impact < thetaLow) {
            return "local_correction";
        } else if (impact < thetaHigh) {
            return "local_replanning";
        } else {
            return "escalate";
        }
    }

    public Map<String, Double> getStatistics() {
        Map<String, Double> stats = new HashMap<>();
        stats.put("mean", mean);
        stats.put("std_dev", stdDev);
        stats.put("window_size", (double) windowSize);
        stats.put("threshold", threshold);
        return stats;
    }
}