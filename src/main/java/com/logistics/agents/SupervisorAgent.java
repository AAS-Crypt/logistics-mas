package com.logistics.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.util.Logger;
import com.logistics.config.ConfigLoader;
import java.util.*;


public class SupervisorAgent extends Agent {
    private Map<AID, Map<String, Double>> managerKPIs = new HashMap<>();
    private long sptaIntervalMs;
    private Map<String, Double> currentPolicy = new HashMap<>();

    protected void setup() {
        ContentManager cm = getContentManager();
        cm.registerLanguage(new SLCodec());
        cm.registerOntology(LogisticsOntology.getInstance());
        Logger.info(getLocalName(), "SupervisorAgent started.");
        sptaIntervalMs = ConfigLoader.getLong("spta.intervalMs", 600000L); 
        currentPolicy.put("mcaa.weight.cost", 0.3);
        currentPolicy.put("mcaa.weight.time", 0.4);
        currentPolicy.put("mcaa.weight.reliability", 0.3);

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                    handleKPIReport(msg);
                } else {
                    block();
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, sptaIntervalMs) {
            protected void onTick() {
                performSPTA();
            }
        });
    }

    private void handleKPIReport(ACLMessage msg) {
        AID manager = msg.getSender();
        String content = msg.getContent();
        Logger.info(getLocalName(), "Received KPI report from: " + manager.getLocalName());
        Map<String, Double> kpis = parseKPIReport(content);
        managerKPIs.put(manager, kpis);
        Logger.info(getLocalName(), "Stored KPIs for " + manager.getLocalName() + ": " + kpis);
    }

    private Map<String, Double> parseKPIReport(String report) {
        Map<String, Double> kpis = new HashMap<>();
        if (report == null || report.isEmpty()) {
            return kpis;
        }
        
        String[] pairs = report.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    kpis.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException e) {
                    Logger.warning(getLocalName(), "Invalid KPI value: " + pair);
                }
            }
        }
        return kpis;
    }

    private void performSPTA() {
        Logger.info(getLocalName(), "SPTA: Starting policy tuning...");
        collectKPIs();
        Map<String, Double> analysis = analyzePerformance();
        Map<String, Double> newPolicy = generatePolicyUpdates(analysis);
        distributePolicyUpdates(newPolicy);
        Logger.info(getLocalName(), "SPTA: Policy tuning completed.");
    }

    private void collectKPIs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("manager");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            Logger.info(getLocalName(), "SPTA: Found " + results.length + " managers.");
            
            for (DFAgentDescription dfd : results) {
                AID manager = dfd.getName();
                requestKPIs(manager);
            }
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "SPTA: Failed to search DF for managers", e);
        }
    }

    private void requestKPIs(AID manager) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(manager);
        request.setLanguage(new SLCodec().getName());
        request.setOntology(LogisticsOntology.getInstance().getName());
        request.setContent("REQUEST_KPIS");
        send(request);
        Logger.info(getLocalName(), "SPTA: Requested KPIs from " + manager.getLocalName());
    }

    private Map<String, Double> analyzePerformance() {
        Map<String, Double> analysis = new HashMap<>();
        
        if (managerKPIs.isEmpty()) {
            Logger.info(getLocalName(), "SPTA: No KPIs available for analysis.");
            return analysis;
        }
        double totalDeliveryTime = 0;
        double totalCost = 0;
        double totalEscalations = 0;
        int count = 0;

        for (Map<String, Double> kpis : managerKPIs.values()) {
            totalDeliveryTime += kpis.getOrDefault("avg_delivery_time", 0.0);
            totalCost += kpis.getOrDefault("avg_cost", 0.0);
            totalEscalations += kpis.getOrDefault("escalations", 0.0);
            count++;
        }

        if (count > 0) {
            analysis.put("avg_delivery_time", totalDeliveryTime / count);
            analysis.put("avg_cost", totalCost / count);
            analysis.put("avg_escalations", totalEscalations / count);
        }

        Logger.info(getLocalName(), "SPTA: Performance analysis: " + analysis);
        return analysis;
    }

     
    private Map<String, Double> generatePolicyUpdates(Map<String, Double> analysis) {
        Map<String, Double> newPolicy = new HashMap<>(currentPolicy);

        if (analysis.isEmpty()) {
            return newPolicy;
        }
        
        Double avgCost = analysis.get("avg_cost");
        if (avgCost != null && avgCost > 1000) {
            double newCostWeight = Math.min(0.5, currentPolicy.get("mcaa.weight.cost") + 0.05);
            newPolicy.put("mcaa.weight.cost", newCostWeight);
            Logger.info(getLocalName(), "SPTA: Increasing cost weight to " + newCostWeight);
        }
        
        Double avgDeliveryTime = analysis.get("avg_delivery_time");
        if (avgDeliveryTime != null && avgDeliveryTime > 24) {
            double newTimeWeight = Math.min(0.6, currentPolicy.get("mcaa.weight.time") + 0.05);
            newPolicy.put("mcaa.weight.time", newTimeWeight);
            Logger.info(getLocalName(), "SPTA: Increasing time weight to " + newTimeWeight);
        }
        
        double sum = newPolicy.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            for (Map.Entry<String, Double> entry : newPolicy.entrySet()) {
                entry.setValue(entry.getValue() / sum);
            }
        }
        currentPolicy = newPolicy;
        return newPolicy;
    }

     
    private void distributePolicyUpdates(Map<String, Double> newPolicy) {
        Logger.info(getLocalName(), "SPTA: Distributing policy updates to managers...");
        StringBuilder policyStr = new StringBuilder();
        for (Map.Entry<String, Double> entry : newPolicy.entrySet()) {
            policyStr.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("manager");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            for (DFAgentDescription dfd : results) {
                AID manager = dfd.getName();
                sendPolicyUpdate(manager, policyStr.toString());
            }
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "SPTA: Failed to distribute policy updates", e);
        }
    }

     
    private void sendPolicyUpdate(AID manager, String policy) {
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(manager);
        inform.setLanguage(new SLCodec().getName());
        inform.setOntology(LogisticsOntology.getInstance().getName());
        inform.setContent("POLICY_UPDATE:" + policy);
        send(inform);
        Logger.info(getLocalName(), "SPTA: Sent policy update to " + manager.getLocalName());
    }
}