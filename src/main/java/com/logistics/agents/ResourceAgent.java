package com.logistics.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Resource;
import com.logistics.ontology.concepts.Event;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.ontology.predicates.SubscribeToEvents;
import com.logistics.ontology.predicates.EventNotification;
import com.logistics.ontology.predicates.ConflictData;
import com.logistics.behaviours.ResourceAuctionResponder;
import com.logistics.util.Logger;
import com.logistics.config.ConfigLoader;
import java.util.*;

public class ResourceAgent extends Agent {
    private Resource myResource;
    private AID activeContractOrder = null; 
    private Map<AID, Order> pendingOrders = new HashMap<>(); 

    protected void setup() {
        ContentManager cm = getContentManager();
        cm.registerLanguage(new SLCodec());
        cm.registerOntology(LogisticsOntology.getInstance());

        Logger.info(getLocalName(), "ResourceAgent started.");
        myResource = createResource();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("resource");
        sd.setName(getLocalName() + "-resource");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            Logger.info(getLocalName(), "Registered with DF.");
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "Failed to register with DF", e);
        }
        
        addBehaviour(new ResourceAuctionResponder(this));
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                    
                    try {
                        Object content = getContentManager().extractContent(msg);
                        if (content instanceof com.logistics.ontology.predicates.ArbitrationResult) {
                            com.logistics.ontology.predicates.ArbitrationResult result = 
                                (com.logistics.ontology.predicates.ArbitrationResult) content;
                            handleArbitrationResult(result);
                        }
                    } catch (Exception e) {
                        
                        if (!(e instanceof ClassCastException)) {
                            Logger.error(getLocalName(), "Failed to process message", e);
                        }
                    }
                } else {
                    block();
                }
            }
        });
        addBehaviour(new SubscribeToMonitorBehaviour(this));
        addBehaviour(new EventHandlingBehaviour(this));
    }

    private Resource createResource() {
        Resource res = new Resource();
        res.setResourceId(getLocalName());
        res.setType("truck");
        res.setCapacityWeight(5000);
        res.setCapacityVolume(20);
        res.setLocation("Almaty");
        res.setCostPerKm(50);
        return res;
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "Failed to deregister from DF", e);
        }
        Logger.info(getLocalName(), "ResourceAgent terminating.");
    }

    private class SubscribeToMonitorBehaviour extends OneShotBehaviour {
        public SubscribeToMonitorBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            AID monitorAID = new AID("monitor", AID.ISLOCALNAME);
            ACLMessage subMsg = new ACLMessage(ACLMessage.SUBSCRIBE);
            subMsg.addReceiver(monitorAID);
            subMsg.setLanguage(new SLCodec().getName());
            subMsg.setOntology(LogisticsOntology.getInstance().getName());

            SubscribeToEvents sub = new SubscribeToEvents();
            sub.setEventTypes("TRAFFIC_JAM");
            sub.setLocation("Almaty");
            sub.setSubscriber(getLocalName());

            try {
                getContentManager().fillContent(subMsg, sub);
                send(subMsg);
                Logger.info(getLocalName(), "Sent subscription to monitor.");
            } catch (Exception e) {
                Logger.error(getLocalName(), "Failed to send subscription", e);
            }
        }
    }
     
    public boolean wouldCreateConflict(AID newOrder) {
        return activeContractOrder != null || !pendingOrders.isEmpty();
    }
     
    public void registerAcceptedOrder(AID orderAID, Order order) {
        pendingOrders.put(orderAID, order);
        Logger.info(getLocalName(), "Registered accepted order: " + orderAID.getLocalName() + 
                   " (total pending: " + pendingOrders.size() + ")");
        if (pendingOrders.size() > 1) {
            escalateConflict();
        }
    }
     
    public void confirmContract(AID orderAID) {
        activeContractOrder = orderAID;
        pendingOrders.clear();
        Logger.info(getLocalName(), "Contract confirmed with: " + orderAID.getLocalName());
    }
     
    private void escalateConflict() {
        if (pendingOrders.size() < 2) {
            return; 
        }
        Logger.warning(getLocalName(), "Conflict detected! Escalating " + pendingOrders.size() + " orders to ManagerAgent");
        ConflictData conflict = new ConflictData();
        conflict.setResource(getAID());
        List<AID> orders = new ArrayList<>(pendingOrders.keySet());
        conflict.setOrders(orders);
        List<Double> winUtils = new ArrayList<>();
        List<Double> loseUtils = new ArrayList<>();
        
        for (AID orderAID : orders) {
            Order order = pendingOrders.get(orderAID);
            double winUtil = calculateWinUtility(order);
            double loseUtil = calculateLoseUtility(order);
            winUtils.add(winUtil);
            loseUtils.add(loseUtil);
        }
        
        conflict.setUtilitiesIfWin(winUtils);
        conflict.setUtilitiesIfLose(loseUtils);
        AID managerAID = new AID("manager", AID.ISLOCALNAME);
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(managerAID);
        request.setLanguage(new SLCodec().getName());
        request.setOntology(LogisticsOntology.getInstance().getName());
        
        try {
            getContentManager().fillContent(request, conflict);
            send(request);
            Logger.info(getLocalName(), "Sent conflict escalation to ManagerAgent");
        } catch (Exception e) {
            Logger.error(getLocalName(), "Failed to send conflict escalation", e);
        }
    }
     
    private double calculateWinUtility(Order order) {
        long now = System.currentTimeMillis();
        long deadline = order.getDeadline().getTime();
        long timeToDeadline = deadline - now;
        if (timeToDeadline <= 0) return -1000; 
        return (1.0 / order.getPriority()) * (1.0 / (timeToDeadline / 3600000.0 + 1));
    }
     
    private double calculateLoseUtility(Order order) {
        return -50.0; 
    }

    private void handleArbitrationResult(com.logistics.ontology.predicates.ArbitrationResult result) {
        AID winner = result.getWinner();
        AID loser = result.getLoser();
        double compensation = result.getCompensation();
        if (pendingOrders.containsKey(winner)) {
            Logger.info(getLocalName(), "Arbitration result: Winner is " + winner.getLocalName() + 
                       ", compensation: " + compensation);
            confirmContract(winner);
        } else if (pendingOrders.containsKey(loser)) {
            Logger.info(getLocalName(), "Arbitration result: Lost to " + winner.getLocalName());
            pendingOrders.remove(loser);
        }
    }
     
    private class EventHandlingBehaviour extends CyclicBehaviour {
        private static final double SEVERITY_LOW = 0.3;
        private static final double SEVERITY_MEDIUM = 0.6;
        private static final double SEVERITY_HIGH = 0.9;
        public EventHandlingBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    try {
                        EventNotification notif = (EventNotification) getContentManager().extractContent(msg);
                        Event event = notif.getEvent();
                        Logger.info(getLocalName(), "Received event: " + event.getType() + " at " + event.getLocation());
                        double impact = calculateImpact(event);
                        Logger.info(getLocalName(), "Calculated impact: " + impact);

                        double thetaLow = ConfigLoader.getDouble("aera.threshold.low", 0.3);
                        double thetaHigh = ConfigLoader.getDouble("aera.threshold.high", 0.7);

                        
                        if (impact < thetaLow) {
                            handleLocalCorrection(event, impact);
                        } else if (impact < thetaHigh) {
                            handleLocalReplanning(event, impact);
                        } else {
                            escalateToManager(event, impact);
                        }
                    } catch (Exception e) {
                        Logger.error(getLocalName(), "Failed to process event notification", e);
                    }
                }
            } else {
                block();
            }
        }
         
        private double calculateImpact(Event event) {
            double severity = mapSeverity(event.getSeverity());
            double distance = calculateDistance(event.getLocation());
            double confidence = 0.95; 
            return severity * distance * confidence;
        }
         
        private double mapSeverity(String severity) {
            if (severity == null) return SEVERITY_MEDIUM;
            
            switch (severity.toUpperCase()) {
                case "LOW": return SEVERITY_LOW;
                case "MEDIUM": return SEVERITY_MEDIUM;
                case "HIGH": return SEVERITY_HIGH;
                default: return SEVERITY_MEDIUM;
            }
        }
         
        private double calculateDistance(String eventLocation) {
            if (eventLocation == null || myResource == null) {
                return 0.5; 
            }
            if (eventLocation.equalsIgnoreCase(myResource.getLocation())) {
                return 1.0;
            }
            if (eventLocation.toLowerCase().contains(myResource.getLocation().toLowerCase()) ||
                myResource.getLocation().toLowerCase().contains(eventLocation.toLowerCase())) {
                return 0.7;
            }
            return 0.3; 
        }
         
        private void handleLocalCorrection(Event event, double impact) {
            Logger.info(getLocalName(), "AERA: Local correction (impact=" + impact + "). Updating ETA for active orders.");
            if (activeContractOrder != null) {
                Logger.info(getLocalName(), "Would update ETA for order: " + activeContractOrder.getLocalName());
            }
        }
         
        private void handleLocalReplanning(Event event, double impact) {
            Logger.warning(getLocalName(), "AERA: Local replanning (impact=" + impact + "). Triggering re-auction.");
            if (activeContractOrder != null) {
                Logger.warning(getLocalName(), "Would trigger re-auction for order: " + activeContractOrder.getLocalName());
            }
        }

        private void escalateToManager(Event event, double impact) {
            Logger.warning(getLocalName(), "AERA: Escalating to ManagerAgent (impact=" + impact + "). Global replanning required.");
            AID managerAID = new AID("manager", AID.ISLOCALNAME);
            ACLMessage escalation = new ACLMessage(ACLMessage.REQUEST);
            escalation.addReceiver(managerAID);
            escalation.setLanguage(new SLCodec().getName());
            escalation.setOntology(LogisticsOntology.getInstance().getName());
            escalation.setContent("AERA_ESCALATION: " + event.getType() + " at " + event.getLocation() + " with impact " + impact);
            
            send(escalation);
            Logger.info(getLocalName(), "Sent AERA escalation to ManagerAgent");
        }
    }
}