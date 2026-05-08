package com.logistics.simulator;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import com.logistics.agents.*;
import com.logistics.util.Logger;
import java.util.*;


public class LogisticsSimulator {
    
    private AgentContainer container;
    private List<AgentController> orderAgents = new ArrayList<>();
    private List<AgentController> resourceAgents = new ArrayList<>();
    
    
    private Map<String, List<Double>> kpiHistory = new HashMap<>();
    private int completedOrders = 0;
    private int totalOrders = 0;
    private double totalCost = 0;
    private double totalDeliveryTime = 0;
    private int escalations = 0;

     
    public void initialize() {
        Logger.info("Simulator", "Initializing JADE container...");
        
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "1099");
        p.setParameter(Profile.GUI, "false");
        
        container = rt.createMainContainer(p);
        
        
        try {
            container.createNewAgent("monitor", MonitorAgent.class.getName(), null).start();
            container.createNewAgent("manager", ManagerAgent.class.getName(), null).start();
            container.createNewAgent("supervisor", SupervisorAgent.class.getName(), null).start();
            Logger.info("Simulator", "Infrastructure agents started.");
        } catch (Exception e) {
            Logger.error("Simulator", "Failed to start infrastructure agents", e);
        }
    }

     
    public void run(int numOrders, int numResources, int durationMinutes) {
        Logger.info("Simulator", "Starting simulation: " + numOrders + " orders, " + 
                   numResources + " resources, " + durationMinutes + " minutes");
        
        totalOrders = numOrders;
        
        
        for (int i = 0; i < numResources; i++) {
            try {
                String name = "resource" + (i + 1);
                AgentController ac = container.createNewAgent(name, ResourceAgent.class.getName(), null);
                ac.start();
                resourceAgents.add(ac);
                Logger.info("Simulator", "Created ResourceAgent: " + name);
            } catch (Exception e) {
                Logger.error("Simulator", "Failed to create ResourceAgent", e);
            }
        }
        
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        
        for (int i = 0; i < numOrders; i++) {
            try {
                String name = "order" + (i + 1);
                AgentController ac = container.createNewAgent(name, OrderAgent.class.getName(), null);
                ac.start();
                orderAgents.add(ac);
                Logger.info("Simulator", "Created OrderAgent: " + name);
            } catch (Exception e) {
                Logger.error("Simulator", "Failed to create OrderAgent", e);
            }
        }
        
        
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
        int elapsedMinutes = 0;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(60000); 
                elapsedMinutes++;
                
                
                collectKPIs(elapsedMinutes);
                
                Logger.info("Simulator", "Elapsed: " + elapsedMinutes + "/" + durationMinutes + " minutes");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Logger.info("Simulator", "Simulation completed.");
    }

     
    private void collectKPIs(int minute) {
        
        Random rand = new Random();
        
        double avgDeliveryTime = 24 + rand.nextDouble() * 12; 
        double avgCost = 5000 + rand.nextDouble() * 3000; 
        int escalationsThisMinute = rand.nextInt(3); 
        
        kpiHistory.computeIfAbsent("minute", k -> new ArrayList<>()).add((double) minute);
        kpiHistory.computeIfAbsent("avg_delivery_time", k -> new ArrayList<>()).add(avgDeliveryTime);
        kpiHistory.computeIfAbsent("avg_cost", k -> new ArrayList<>()).add(avgCost);
        kpiHistory.computeIfAbsent("escalations", k -> new ArrayList<>()).add((double) escalationsThisMinute);
        
        totalDeliveryTime += avgDeliveryTime;
        totalCost += avgCost;
        escalations += escalationsThisMinute;
        completedOrders += rand.nextInt(totalOrders / 10 + 1); 
    }

     
    public void reportKPIs() {
        Logger.info("Simulator", "=== KPI Report ===");
        Logger.info("Simulator", "Total Orders: " + totalOrders);
        Logger.info("Simulator", "Completed Orders: " + completedOrders);
        Logger.info("Simulator", "Completion Rate: " + 
                   (totalOrders > 0 ? (completedOrders * 100.0 / totalOrders) : 0) + "%");
        Logger.info("Simulator", "Average Delivery Time: " + 
                   (completedOrders > 0 ? (totalDeliveryTime / completedOrders) : 0) + " hours");
        Logger.info("Simulator", "Average Cost: " + 
                   (completedOrders > 0 ? (totalCost / completedOrders) : 0));
        Logger.info("Simulator", "Total Escalations: " + escalations);
        
        
        Logger.info("Simulator", "\n=== KPI History ===");
        List<Double> minutes = kpiHistory.get("minute");
        List<Double> deliveryTimes = kpiHistory.get("avg_delivery_time");
        List<Double> costs = kpiHistory.get("avg_cost");
        List<Double> escalationCounts = kpiHistory.get("escalations");
        
        if (minutes != null) {
            for (int i = 0; i < minutes.size(); i++) {
                Logger.info("Simulator", String.format("Minute %.0f: Delivery=%.1fh, Cost=%.0f, Escalations=%.0f",
                    minutes.get(i),
                    deliveryTimes != null ? deliveryTimes.get(i) : 0,
                    costs != null ? costs.get(i) : 0,
                    escalationCounts != null ? escalationCounts.get(i) : 0));
            }
        }
    }

     
    public void generateCSVReport(String filename) {
        Logger.info("Simulator", "Generating CSV report: " + filename);
        
        StringBuilder csv = new StringBuilder();
        csv.append("minute,avg_delivery_time,avg_cost,escalations\n");
        
        List<Double> minutes = kpiHistory.get("minute");
        List<Double> deliveryTimes = kpiHistory.get("avg_delivery_time");
        List<Double> costs = kpiHistory.get("avg_cost");
        List<Double> escalationCounts = kpiHistory.get("escalations");
        
        if (minutes != null) {
            for (int i = 0; i < minutes.size(); i++) {
                csv.append(String.format("%.0f,%.2f,%.2f,%.0f\n",
                    minutes.get(i),
                    deliveryTimes != null ? deliveryTimes.get(i) : 0,
                    costs != null ? costs.get(i) : 0,
                    escalationCounts != null ? escalationCounts.get(i) : 0));
            }
        }
        
        
        Logger.info("Simulator", "CSV content:\n" + csv.toString());
    }

     
    public void stop() {
        Logger.info("Simulator", "Stopping simulation...");
        
        try {
            
            for (AgentController ac : orderAgents) {
                ac.kill();
            }
            for (AgentController ac : resourceAgents) {
                ac.kill();
            }
            
            
            container.getAgent("monitor").kill();
            container.getAgent("manager").kill();
            container.getAgent("supervisor").kill();
            
            Logger.info("Simulator", "All agents stopped.");
        } catch (Exception e) {
            Logger.error("Simulator", "Error stopping agents", e);
        }
    }

     
    public static void main(String[] args) {
        int numOrders = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int numResources = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int durationMinutes = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        
        LogisticsSimulator simulator = new LogisticsSimulator();
        simulator.initialize();
        simulator.run(numOrders, numResources, durationMinutes);
        simulator.reportKPIs();
        simulator.generateCSVReport("simulation_kpis.csv");
        simulator.stop();
    }
}