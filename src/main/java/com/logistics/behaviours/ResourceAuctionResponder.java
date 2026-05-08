package com.logistics.behaviours;
import java.util.Date;
import java.util.Random;
import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.CallForProposal;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.agents.ResourceAgent;
import com.logistics.util.Logger;


public class ResourceAuctionResponder extends ContractNetResponder {
    private Order currentOrder = null;

    public ResourceAuctionResponder(Agent a) {
        super(a, MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.CFP),
            MessageTemplate.MatchProtocol(jade.domain.FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)
        ));
    }

    protected ACLMessage handleCfp(ACLMessage cfp) {
        Order requestedOrder = null;
        try {
            CallForProposal cfpContent = (CallForProposal) myAgent.getContentManager().extractContent(cfp);
            requestedOrder = cfpContent.getOrder();
            Logger.info(myAgent.getLocalName(), "Received CFP for order " + requestedOrder.getOrderId() + " from " + cfp.getSender().getLocalName() + " (AID: " + cfp.getSender().getName() + ")");
        } catch (Exception e) {
            Logger.error(myAgent.getLocalName(), "Failed to decode CFP from " + cfp.getSender().getLocalName(), e);
            
            ACLMessage refuse = cfp.createReply();
            refuse.setPerformative(ACLMessage.REFUSE);
            refuse.setContent("Failed to decode CFP");
            return refuse;
        }

        boolean canHandle = checkFeasibility(requestedOrder);
        Logger.info(myAgent.getLocalName(), "Can handle order " + (requestedOrder != null ? requestedOrder.getOrderId() : "unknown") + ": " + canHandle);

        ACLMessage reply = cfp.createReply();
        if (canHandle) {
            
            try {
                Random rand = new Random();
                int delay = rand.nextInt(20); 
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                
            }
            
            Proposal proposal = createProposal(requestedOrder);
            try {
                myAgent.getContentManager().fillContent(reply, proposal);
                Logger.info(myAgent.getLocalName(), "Sending PROPOSE for order " + requestedOrder.getOrderId() + " to " + cfp.getSender().getLocalName() + " (AID: " + cfp.getSender().getName() + ") after small delay");
            } catch (Exception e) {
                Logger.error(myAgent.getLocalName(), "Failed to encode proposal for order " + requestedOrder.getOrderId(), e);
                
                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                refuse.setContent("Failed to encode proposal");
                return refuse;
            }
            reply.setPerformative(ACLMessage.PROPOSE);
        } else {
            reply.setPerformative(ACLMessage.REFUSE);
            Logger.info(myAgent.getLocalName(), "Sending REFUSE for order " + (requestedOrder != null ? requestedOrder.getOrderId() : "unknown"));
        }
        return reply;
    }

    private boolean checkFeasibility(Order order) {
        
        return true;
    }

    private Proposal createProposal(Order order) {
        currentOrder = order; 
        Proposal prop = new Proposal();
        prop.setOrder(order);
        prop.setResource(myAgent.getAID());
        prop.setPrice(15000);
        prop.setEstimatedDelivery(new Date(System.currentTimeMillis() + 24*3600*1000));
        prop.setReliability(0.95);
        return prop;
    }

    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
        AID orderAID = accept.getSender();
        Logger.info(myAgent.getLocalName(), "Received ACCEPT_PROPOSAL from: " + orderAID.getLocalName());

        
        ResourceAgent resourceAgent = (ResourceAgent) myAgent;
        
        if (resourceAgent.wouldCreateConflict(orderAID)) {
            
            resourceAgent.registerAcceptedOrder(orderAID, currentOrder);
            
            
            
            return null;
        } else {
            
            resourceAgent.confirmContract(orderAID);
            
            Logger.info(myAgent.getLocalName(), "Contract confirmed with: " + orderAID.getLocalName());
            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("execution started");
            return inform;
        }
    }

    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        Logger.info(myAgent.getLocalName(), "Lost auction.");
    }
}