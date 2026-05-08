package com.logistics.agents;
import java.util.Date;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.*;
import com.logistics.ontology.predicates.ArbitrationResult;
import com.logistics.behaviours.OrderAuctionInitiator;
import com.logistics.util.Logger;
import com.logistics.config.ConfigLoader;


public class OrderAgent extends Agent {
    private Order currentOrder;
    private boolean auctionStarted = false;
    private int maxRetries;
    private int retryCount = 0;
    private long retryIntervalMs;

    protected void setup() {
        
        ContentManager cm = getContentManager();
        cm.registerLanguage(new SLCodec());
        cm.registerOntology(LogisticsOntology.getInstance());

        Logger.info(getLocalName(), "OrderAgent started.");
        maxRetries = ConfigLoader.getInt("order.maxRetries", 5);
        retryIntervalMs = ConfigLoader.getLong("order.retryIntervalMs", 2000L);
        long searchDelayMs = ConfigLoader.getLong("order.searchDelayMs", 1000L);
        int minExpectedResources = ConfigLoader.getInt("order.minExpectedResources", 2);
        currentOrder = createSampleOrder();
        
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                    try {
                        ArbitrationResult result = (ArbitrationResult) getContentManager().extractContent(msg);
                        handleArbitrationResult(result);
                    } catch (Exception e) {
                        Logger.error(getLocalName(), "Failed to process arbitration result", e);
                    }
                } else {
                    block();
                }
            }
        });
        
        addBehaviour(new WakerBehaviour(this, searchDelayMs) {
            protected void onWake() {
                Logger.info(getLocalName(), "Initial delay completed, starting resource search.");
                searchForResourcesWithRetry(minExpectedResources);
            }
        });
    }

     
    private void searchForResourcesWithRetry() {
        searchForResourcesWithRetry(0); 
    }

     
    private void searchForResourcesWithRetry(int minExpectedResources) {
        if (auctionStarted) {
            return; 
        }
        
        if (retryCount >= maxRetries) {
            Logger.warning(getLocalName(), "No resource agents found after " + maxRetries + " attempts. Giving up.");
            return;
        }
        
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("resource");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            Logger.info(getLocalName(), "DF search returned " + results.length + " resource(s).");
            for (DFAgentDescription desc : results) {
                Logger.info(getLocalName(), "Found resource: " + desc.getName().getLocalName() + " (AID: " + desc.getName().getName() + ")");
            }
            
            boolean enoughResources = results.length > 0;
            if (minExpectedResources > 0) {
                enoughResources = results.length >= minExpectedResources;
                if (!enoughResources) {
                    Logger.info(getLocalName(), "Found " + results.length + " resources but expected at least " + minExpectedResources + ". Will retry.");
                }
            }
            
            if (enoughResources) {
                Logger.info(getLocalName(), "Found " + results.length + " resource agent(s). Starting auction.");
                
                addBehaviour(new OrderAuctionInitiator(this, currentOrder, results));
                auctionStarted = true;
            } else {
                retryCount++;
                Logger.info(getLocalName(), "No resource agents found yet (attempt " + retryCount + "/" + maxRetries + "). Retrying in " + retryIntervalMs + "ms.");
                
                addBehaviour(new WakerBehaviour(this, retryIntervalMs) {
                    protected void onWake() {
                        searchForResourcesWithRetry(minExpectedResources);
                    }
                });
            }
        } catch (FIPAException e) {
            Logger.error(getLocalName(), "Failed to search DF for resource agents", e);
        }
    }

    private Order createSampleOrder() {
        Cargo cargo = new Cargo();
        cargo.setCargoId("C001");
        cargo.setType("general");
        cargo.setWeight(1000);
        cargo.setVolume(5);
        
        cargo.setMaxTemperature(30); 

        Location origin = new Location();
        origin.setCity("Almaty");
        Location dest = new Location();
        dest.setCity("Nur-Sultan");

        Order order = new Order();
        order.setOrderId("ORD001");
        order.setCargo(cargo);
        order.setOrigin(origin);
        order.setDestination(dest);
        order.setDeadline(new Date(System.currentTimeMillis() + 48*3600*1000));
        order.setPriority(1);
        order.setMaxBudget(300000);
        return order;
    }
    
    public double getUtilityIfWin() {
        
        long now = System.currentTimeMillis();
        long timeToDeadline = currentOrder.getDeadline().getTime() - now;
        if (timeToDeadline <= 0) return -1000; 
        return (1.0 / currentOrder.getPriority()) * (1.0 / (timeToDeadline / 3600000.0 + 1));
    }

    
    public double getUtilityIfLose() {
        return -50.0; 
    }
    protected void takeDown() {
        Logger.info(getLocalName(), "OrderAgent terminating.");
    }

    private void handleArbitrationResult(ArbitrationResult result) {
        if (result.getWinner().equals(getAID())) {
            Logger.info(getLocalName(), "Won arbitration. Compensation to pay: " + result.getCompensation());
            
        } else if (result.getLoser().equals(getAID())) {
            Logger.info(getLocalName(), "Lost arbitration. Compensation received: " + result.getCompensation());
            
        }
    }
}