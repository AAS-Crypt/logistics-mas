package com.logistics.analytics;

import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;


public class ReportGenerator {
    
     
    public enum ReportType {
        PERFORMANCE, ALGORITHM_COMPARISON, KPI_SUMMARY, SIMULATION_RESULTS
    }
    
     
    public static class Report {
        private String title;
        private ReportType type;
        private Date generatedAt;
        private Map<String, Object> data;
        private List<String> sections;
        
        public Report(String title, ReportType type) {
            this.title = title;
            this.type = type;
            this.generatedAt = new Date();
            this.data = new HashMap<>();
            this.sections = new ArrayList<>();
        }
        
        
        public String getTitle() { return title; }
        public ReportType getType() { return type; }
        public Date getGeneratedAt() { return generatedAt; }
        public Map<String, Object> getData() { return data; }
        public List<String> getSections() { return sections; }
        
        public void addSection(String section) {
            sections.add(section);
        }
        
        public void addData(String key, Object value) {
            data.put(key, value);
        }
    }
    
     
    public static Report generatePerformanceReport(Map<String, List<Double>> kpiHistory, 
                                                   Map<String, Object> simulationResults) {
        Report report = new Report("Performance Report", ReportType.PERFORMANCE);
        
        
        report.addSection("KPI Summary");
        for (Map.Entry<String, List<Double>> entry : kpiHistory.entrySet()) {
            List<Double> values = entry.getValue();
            if (!values.isEmpty()) {
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                
                report.addData(entry.getKey() + "_avg", avg);
                report.addData(entry.getKey() + "_min", min);
                report.addData(entry.getKey() + "_max", max);
            }
        }
        
        
        if (simulationResults != null) {
            report.addSection("Simulation Results");
            for (Map.Entry<String, Object> entry : simulationResults.entrySet()) {
                report.addData(entry.getKey(), entry.getValue());
            }
        }
        
        return report;
    }
    
     
    public static Report generateAlgorithmComparisonReport(Map<String, Map<String, Double>> algorithmResults) {
        Report report = new Report("Algorithm Comparison Report", ReportType.ALGORITHM_COMPARISON);
        
        report.addSection("Algorithm Performance Comparison");
        
        
        Map<String, String> bestAlgorithms = new HashMap<>();
        Map<String, Double> bestScores = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Double>> algoEntry : algorithmResults.entrySet()) {
            String algoName = algoEntry.getKey();
            Map<String, Double> metrics = algoEntry.getValue();
            
            for (Map.Entry<String, Double> metricEntry : metrics.entrySet()) {
                String metric = metricEntry.getKey();
                double score = metricEntry.getValue();
                
                if (!bestScores.containsKey(metric) || score > bestScores.get(metric)) {
                    bestScores.put(metric, score);
                    bestAlgorithms.put(metric, algoName);
                }
            }
        }
        
        
        report.addData("algorithm_results", algorithmResults);
        report.addData("best_algorithms", bestAlgorithms);
        report.addData("best_scores", bestScores);
        
        return report;
    }
    
     
    public static void exportToCSV(Report report, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            
            writer.println("Report: " + report.getTitle());
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(report.getGeneratedAt()));
            writer.println();
            
            
            writer.println("Metric,Value");
            for (Map.Entry<String, Object> entry : report.getData().entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
            
            System.out.println("Report exported to " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting report: " + e.getMessage());
        }
    }
    
     
    public static String exportToJSON(Report report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"title\": \"").append(report.getTitle()).append("\",\n");
        json.append("  \"type\": \"").append(report.getType()).append("\",\n");
        json.append("  \"generated_at\": \"").append(report.getGeneratedAt()).append("\",\n");
        json.append("  \"data\": {\n");
        
        int i = 0;
        for (Map.Entry<String, Object> entry : report.getData().entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
            if (i < report.getData().size() - 1) {
                json.append(",");
            }
            json.append("\n");
            i++;
        }
        
        json.append("  },\n");
        json.append("  \"sections\": [\n");
        
        for (int j = 0; j < report.getSections().size(); j++) {
            json.append("    \"").append(report.getSections().get(j)).append("\"");
            if (j < report.getSections().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
     
    public static String generateHTMLReport(Report report) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <title>").append(report.getTitle()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    h1 { color: #333; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("    th { background-color: #4CAF50; color: white; }\n");
        html.append("    tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("  <h1>").append(report.getTitle()).append("</h1>\n");
        html.append("  <p>Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(report.getGeneratedAt())).append("</p>\n");
        
        
        for (String section : report.getSections()) {
            html.append("  <h2>").append(section).append("</h2>\n");
        }
        
        
        html.append("  <table>\n");
        html.append("    <tr><th>Metric</th><th>Value</th></tr>\n");
        
        for (Map.Entry<String, Object> entry : report.getData().entrySet()) {
            html.append("    <tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>\n");
        }
        
        html.append("  </table>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
}