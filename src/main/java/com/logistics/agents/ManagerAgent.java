package com.logistics.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ACLCodec;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.ConflictData;
import com.logistics.ontology.predicates.ArbitrationResult;
import com.logistics.util.Logger;
import com.logistics.config.ConfigLoader;
import java.util.*;

public class ManagerAgent extends Agent {
    private List<AID> knownResources = new ArrayList<>();
    private Map<AID, Order> activeOrders = new HashMap<>();
    private long pcraIntervalMs;

    protected void setup() {
        
        ContentManager cm = getContentManager();
        cm.registerLanguage(new SLCodec());
        cm.registerOntology(LogisticsOntology.getInstance());

        Logger.info(getLocalName(), "ManagerAgent started.");

        
        pcraIntervalMs = ConfigLoader.getLong("pcra.intervalMs", 300000L); 

        
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        String content = msg.getContent();
                        if (content != null && content.startsWith("AERA_ESCALATION")) {
                            
                            handleAeraEscalation(msg);
                        } else {
                            
                            try {
                                ConflictData conflict = (ConflictData) getContentManager().extractContent(msg);
                                handleConflict(conflict, msg.getSender());
                            } catch (Exception e) {
                                Logger.error(getLocalName(), "Failed to process conflict request", e);
                            }
                        }
                    }
                } else {
                    block();
                }
            }
        });

        
        addBehaviour(new TickerBehaviour(this, pcraIntervalMs) {
            protected void onTick() {
                performPCRA();
            }
        });
    }

     
    private void handleAeraEscalation(ACLMessage msg) {
        Logger.info(getLocalName(), "Received AERA escalation: " + msg.getContent());
    }

     
    private void performPCRA() {
        Logger.info(getLocalName(), "PCRA: Starting periodic cluster replanning...");
        collectClusterState();
        int numScenarios = ConfigLoader.getInt("pcra.numScenarios", 100);
        List<Map<String, Object>> scenarios = generateScenarios(numScenarios);
        Map<String, Object> solution = solveDeterministicEquivalent(scenarios);
        applyFirstStageDecisions(solution);
        Logger.info(getLocalName(), "PCRA: Cluster replanning completed.");
    }

     
    private void collectClusterState() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("resource");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            knownResources.clear();
            for (DFAgentDescription dfd : results) {
                knownResources.add(dfd.getName());
            }
            Logger.info(getLocalName(), "PCRA: Found " + knownResources.size() + " resources in cluster.");
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "PCRA: Failed to search DF for resources", e);
        }
    }

     
    private List<Map<String, Object>> generateScenarios(int numScenarios) {
        List<Map<String, Object>> scenarios = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < numScenarios; i++) {
            Map<String, Object> scenario = new HashMap<>();
            for (AID resource : knownResources) {
                double demand = 0.5 + rand.nextDouble() * 1.0; 
                scenario.put(resource.getLocalName() + "_demand", demand);
            }
            scenarios.add(scenario);
        }
        Logger.info(getLocalName(), "PCRA: Generated " + numScenarios + " scenarios.");
        return scenarios;
    }

     
    private Map<String, Object> solveDeterministicEquivalent(List<Map<String, Object>> scenarios) {
        Map<String, Object> solution = new HashMap<>();
        Map<AID, Double> avgDemand = new HashMap<>();
        for (AID resource : knownResources) {
            double total = 0;
            for (Map<String, Object> scenario : scenarios) {
                total += (Double) scenario.get(resource.getLocalName() + "_demand");
            }
            avgDemand.put(resource, total / scenarios.size());
        }
        List<Map.Entry<AID, Double>> sortedResources = new ArrayList<>(avgDemand.entrySet());
        sortedResources.sort(Map.Entry.comparingByValue());
        
        int orderIndex = 0;
        for (Map.Entry<AID, Double> entry : sortedResources) {
            if (orderIndex < activeOrders.size()) {
                AID orderAID = (AID) activeOrders.keySet().toArray()[orderIndex];
                solution.put("assign_" + orderAID.getLocalName() + "_to", entry.getKey().getLocalName());
                orderIndex++;
            }
        }
        Logger.info(getLocalName(), "PCRA: Solved deterministic equivalent with " + solution.size() + " assignments.");
        return solution;
    }

     
    private void applyFirstStageDecisions(Map<String, Object> solution) {
        Logger.info(getLocalName(), "PCRA: Applying first-stage decisions...");
        for (Map.Entry<String, Object> entry : solution.entrySet()) {
            Logger.info(getLocalName(), "PCRA Decision: " + entry.getKey() + " = " + entry.getValue());
        }
    }

     
    private void handleConflict(ConflictData conflict, AID resource) {
        List<AID> orders = conflict.getOrders();
        List<Double> winUtils = conflict.getUtilitiesIfWin();
        List<Double> loseUtils = conflict.getUtilitiesIfLose();

        if (orders.size() != 2) {
            Logger.warning(getLocalName(), "CAGA currently only supports two competing orders.");
            return;
        }
        
        double u1_win = winUtils.get(0);
        double u1_lose = loseUtils.get(0);
        double u2_win = winUtils.get(1);
        double u2_lose = loseUtils.get(1);
        
        double gain1 = u1_win - u1_lose;
        double gain2 = u2_win - u2_lose;

        AID winner;
        AID loser;
        double surplus;
        if (gain1 >= gain2) {
            winner = orders.get(0);
            loser = orders.get(1);
            surplus = gain1;
        } else {
            winner = orders.get(1);
            loser = orders.get(0);
            surplus = gain2;
        }
        double compensation = surplus / 2;   
        
        ArbitrationResult result = new ArbitrationResult();
        result.setWinner(winner);
        result.setLoser(loser);
        result.setCompensation(compensation);
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(winner);
        msg.addReceiver(loser);
        msg.setLanguage(new SLCodec().getName());
        msg.setOntology(LogisticsOntology.getInstance().getName());
        try {
            getContentManager().fillContent(msg, result);
            send(msg);
            Logger.info(getLocalName(), "Sent arbitration result: winner=" + winner.getLocalName() +
                               ", loser=" + loser.getLocalName() + ", compensation=" + compensation);
        } catch (Exception e) {
            Logger.error(getLocalName(), "Failed to send arbitration result", e);
        }
    }
}