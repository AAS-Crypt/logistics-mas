package com.logistics.algorithms;

import java.util.*;


public class LoadBalancer {
    
     
    public static class Container {
        private String containerId;
        private int agentCount;
        private double cpuUsage;
        private double memoryUsage;
        private double networkLatency;
        
        public Container(String containerId) {
            this.containerId = containerId;
            this.agentCount = 0;
            this.cpuUsage = 0;
            this.memoryUsage = 0;
            this.networkLatency = 0;
        }
        
        
        public String getContainerId() { return containerId; }
        public int getAgentCount() { return agentCount; }
        public void setAgentCount(int count) { this.agentCount = count; }
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double usage) { this.cpuUsage = usage; }
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double usage) { this.memoryUsage = usage; }
        public double getNetworkLatency() { return networkLatency; }
        public void setNetworkLatency(double latency) { this.networkLatency = latency; }
        
         
        public double getLoadScore() {
            return (agentCount * 0.3 + cpuUsage * 0.3 + memoryUsage * 0.3 + networkLatency * 0.1);
        }
    }
    
    private List<Container> containers;
    private String strategy; 
    
    public LoadBalancer(String strategy) {
        this.containers = new ArrayList<>();
        this.strategy = strategy;
    }
    
     
    public void addContainer(String containerId) {
        containers.add(new Container(containerId));
    }
    
     
    public void updateContainerMetrics(String containerId, int agentCount, 
                                       double cpuUsage, double memoryUsage, 
                                       double networkLatency) {
        for (Container container : containers) {
            if (container.getContainerId().equals(containerId)) {
                container.setAgentCount(agentCount);
                container.setCpuUsage(cpuUsage);
                container.setMemoryUsage(memoryUsage);
                container.setNetworkLatency(networkLatency);
                break;
            }
        }
    }
    
     
    public String selectContainer(String agentType) {
        if (containers.isEmpty()) {
            return null;
        }
        
        switch (strategy) {
            case "round_robin":
                return selectRoundRobin();
            case "least_loaded":
                return selectLeastLoaded();
            case "weighted":
                return selectWeighted();
            default:
                return selectLeastLoaded();
        }
    }
    
     
    private int roundRobinIndex = 0;
    
    private String selectRoundRobin() {
        String containerId = containers.get(roundRobinIndex).getContainerId();
        roundRobinIndex = (roundRobinIndex + 1) % containers.size();
        return containerId;
    }
    
     
    private String selectLeastLoaded() {
        Container leastLoaded = Collections.min(containers, 
            Comparator.comparingDouble(Container::getLoadScore));
        return leastLoaded.getContainerId();
    }
    
     
    private String selectWeighted() {
        
        double totalInverseLoad = 0;
        for (Container container : containers) {
            totalInverseLoad += 1.0 / (container.getLoadScore() + 1);
        }
        
        
        Random rand = new Random();
        double randomValue = rand.nextDouble() * totalInverseLoad;
        
        double cumulative = 0;
        for (Container container : containers) {
            cumulative += 1.0 / (container.getLoadScore() + 1);
            if (randomValue <= cumulative) {
                return container.getContainerId();
            }
        }
        
        return containers.get(0).getContainerId();
    }
    
     
    public Map<String, Map<String, Double>> getLoadStatistics() {
        Map<String, Map<String, Double>> stats = new HashMap<>();
        
        for (Container container : containers) {
            Map<String, Double> containerStats = new HashMap<>();
            containerStats.put("agent_count", (double) container.getAgentCount());
            containerStats.put("cpu_usage", container.getCpuUsage());
            containerStats.put("memory_usage", container.getMemoryUsage());
            containerStats.put("network_latency", container.getNetworkLatency());
            containerStats.put("load_score", container.getLoadScore());
            
            stats.put(container.getContainerId(), containerStats);
        }
        
        return stats;
    }
    
     
    public boolean needsRebalancing() {
        if (containers.size() < 2) return false;
        
        double maxLoad = Collections.max(containers, 
            Comparator.comparingDouble(Container::getLoadScore)).getLoadScore();
        double minLoad = Collections.min(containers, 
            Comparator.comparingDouble(Container::getLoadScore)).getLoadScore();
        
        
        return (maxLoad - minLoad) / maxLoad > 0.3;
    }
    
     
    public List<Container> getContainers() {
        return containers;
    }
}