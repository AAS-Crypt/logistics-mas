package com.logistics.algorithms;

import java.util.*;


public class PredictiveMaintenance {
    
     
    public static class HealthStatus {
        private String resourceId;
        private double healthScore; 
        private double failureProbability;
        private long estimatedTimeToFailure; 
        private List<String> issues;
        
        public HealthStatus(String resourceId) {
            this.resourceId = resourceId;
            this.healthScore = 1.0;
            this.failureProbability = 0.0;
            this.estimatedTimeToFailure = Long.MAX_VALUE;
            this.issues = new ArrayList<>();
        }
        
        
        public String getResourceId() { return resourceId; }
        public double getHealthScore() { return healthScore; }
        public void setHealthScore(double score) { this.healthScore = score; }
        public double getFailureProbability() { return failureProbability; }
        public void setFailureProbability(double prob) { this.failureProbability = prob; }
        public long getEstimatedTimeToFailure() { return estimatedTimeToFailure; }
        public void setEstimatedTimeToFailure(long hours) { this.estimatedTimeToFailure = hours; }
        public List<String> getIssues() { return issues; }
        public void addIssue(String issue) { issues.add(issue); }
    }
    
     
    public static class MaintenanceRecord {
        private String resourceId;
        private long timestamp;
        private String maintenanceType;
        private double cost;
        private String issue;
        
        public MaintenanceRecord(String resourceId, long timestamp, String type, double cost, String issue) {
            this.resourceId = resourceId;
            this.timestamp = timestamp;
            this.maintenanceType = type;
            this.cost = cost;
            this.issue = issue;
        }
        
        
        public String getResourceId() { return resourceId; }
        public long getTimestamp() { return timestamp; }
        public String getMaintenanceType() { return maintenanceType; }
        public double getCost() { return cost; }
        public String getIssue() { return issue; }
    }
    
    private Map<String, List<MaintenanceRecord>> maintenanceHistory;
    private Map<String, HealthStatus> healthStatuses;
    
    public PredictiveMaintenance() {
        this.maintenanceHistory = new HashMap<>();
        this.healthStatuses = new HashMap<>();
    }
    
     
    public void updateHealth(String resourceId, double usageHours, int failureCount, 
                            double lastMaintenanceHours) {
        HealthStatus status = healthStatuses.computeIfAbsent(resourceId, HealthStatus::new);
        
        
        double usageFactor = Math.max(0, 1.0 - (usageHours / 10000.0));
        double failureFactor = Math.max(0, 1.0 - (failureCount * 0.1));
        double maintenanceFactor = Math.max(0, 1.0 - (lastMaintenanceHours / 5000.0));
        
        double healthScore = (usageFactor * 0.4 + failureFactor * 0.3 + maintenanceFactor * 0.3);
        status.setHealthScore(healthScore);
        
        
        double shape = 1.5; 
        double scale = 8000.0; 
        double failureProb = 1 - Math.exp(-Math.pow(usageHours / scale, shape));
        status.setFailureProbability(failureProb);
        
        
        if (failureProb > 0.8) {
            status.setEstimatedTimeToFailure(24); 
            status.addIssue("Critical: High failure probability");
        } else if (failureProb > 0.5) {
            status.setEstimatedTimeToFailure(168); 
            status.addIssue("Warning: Moderate failure probability");
        } else if (failureProb > 0.2) {
            status.setEstimatedTimeToFailure(720); 
            status.addIssue("Notice: Elevated failure probability");
        }
    }
    
     
    public List<String> predictMaintenanceNeeds() {
        List<String> needsMaintenance = new ArrayList<>();
        
        for (Map.Entry<String, HealthStatus> entry : healthStatuses.entrySet()) {
            HealthStatus status = entry.getValue();
            
            
            if (status.getFailureProbability() > 0.5 || status.getHealthScore() < 0.5) {
                needsMaintenance.add(entry.getKey());
            }
        }
        
        return needsMaintenance;
    }
    
     
    public Map<String, Long> generateMaintenanceSchedule(long currentTime) {
        Map<String, Long> schedule = new HashMap<>();
        
        for (Map.Entry<String, HealthStatus> entry : healthStatuses.entrySet()) {
            HealthStatus status = entry.getValue();
            
            if (status.getEstimatedTimeToFailure() < Long.MAX_VALUE) {
                long maintenanceTime = currentTime + (status.getEstimatedTimeToFailure() * 3600000L);
                schedule.put(entry.getKey(), maintenanceTime);
            }
        }
        
        return schedule;
    }
    
     
    public HealthStatus getHealthStatus(String resourceId) {
        return healthStatuses.get(resourceId);
    }
    
     
    public Map<String, HealthStatus> getAllHealthStatuses() {
        return healthStatuses;
    }
    
     
    public double estimateMaintenanceCost(String resourceId) {
        HealthStatus status = healthStatuses.get(resourceId);
        if (status == null) return 0;
        
        
        double baseCost = 1000;
        double urgencyMultiplier = 1.0 + (1.0 - status.getHealthScore()) * 2.0;
        
        return baseCost * urgencyMultiplier;
    }
}