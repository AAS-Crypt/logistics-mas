package com.logistics.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Order;
import com.logistics.ontology.predicates.CallForProposal;
import com.logistics.ontology.predicates.Proposal;
import com.logistics.algorithms.MCAA;
import com.logistics.util.Logger;

import java.util.Vector;
import java.util.Date;
import java.util.Enumeration;

public class OrderAuctionInitiator extends ContractNetInitiator {
    private Order order;
    private DFAgentDescription[] resourceDescriptions;

    public OrderAuctionInitiator(Agent a, Order order, DFAgentDescription[] resources) {
        super(a, createCfp(a, order, resources));
        this.order = order;
        this.resourceDescriptions = resources;
        Logger.info(a.getLocalName(), "OrderAuctionInitiator created with " + resources.length + " receivers.");
    }

    private static ACLMessage createCfp(Agent agent, Order order, DFAgentDescription[] resources) {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (DFAgentDescription dfd : resources) {
            cfp.addReceiver(dfd.getName());
        }
        cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        cfp.setLanguage(new SLCodec().getName());
        cfp.setOntology(LogisticsOntology.getInstance().getName());
        CallForProposal cfpContent = new CallForProposal(order);
        try {
            agent.getContentManager().fillContent(cfp, cfpContent);
            Logger.info(agent.getLocalName(), "CFP content filled successfully for order " + order.getOrderId());
        } catch (Exception ex) {
            Logger.error(agent.getLocalName(), "Failed to fill CFP content", ex);
            ex.printStackTrace();
        }
        cfp.setReplyByDate(new Date(System.currentTimeMillis() + 30000)); 
        int receiverCount = 0;
        jade.util.leap.Iterator it = cfp.getAllReceiver();
        while (it.hasNext()) {
            it.next();
            receiverCount++;
        }
        Logger.info(agent.getLocalName(), "CFP created with reply deadline: " + cfp.getReplyByDate() + ", receivers: " + receiverCount);
        return cfp;
    }

    @SuppressWarnings("unchecked")
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        double bestScore = -1;
        ACLMessage bestProposal = null;
        Proposal bestProposalData = null;
        int proposalCount = 0;
        int totalMessages = 0;

        Logger.info(myAgent.getLocalName(), "Processing proposals for order " + order.getOrderId());
        Logger.info(myAgent.getLocalName(), "Total responses in vector: " + responses.size());

        Enumeration<ACLMessage> e = responses.elements();
        while (e.hasMoreElements()) {
            ACLMessage msg = e.nextElement();
            totalMessages++;
            Logger.info(myAgent.getLocalName(), "Response #" + totalMessages + " from " + msg.getSender().getLocalName() + 
                       " performative: " + ACLMessage.getPerformative(msg.getPerformative()));
            
            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                proposalCount++;
                try {
                    Proposal prop = (Proposal) myAgent.getContentManager().extractContent(msg);
                    double score = MCAA.computeScore(order, prop);
                    Logger.info(myAgent.getLocalName(), "Received proposal from " + msg.getSender().getLocalName() + " with score " + score);
                    if (score > bestScore) {
                        bestScore = score;
                        bestProposal = msg;
                        bestProposalData = prop;
                    }
                } catch (Exception ex2) {
                    Logger.error(myAgent.getLocalName(), "Failed to extract proposal content from " + msg.getSender().getLocalName(), ex2);
                }
            } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                Logger.info(myAgent.getLocalName(), "Received REFUSE from " + msg.getSender().getLocalName());
            } else {
                Logger.info(myAgent.getLocalName(), "Unexpected message type from " + msg.getSender().getLocalName() + 
                           ": " + ACLMessage.getPerformative(msg.getPerformative()));
            }
        }

        Logger.info(myAgent.getLocalName(), "Total messages: " + totalMessages + ", Proposals: " + proposalCount);

        if (bestProposal != null) {
            ACLMessage accept = bestProposal.createReply();
            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            acceptances.addElement(accept);
            Logger.info(myAgent.getLocalName(), "Order " + order.getOrderId() + " allocated to " + bestProposal.getSender().getLocalName() + " with score " + bestScore);
        }

        Enumeration<ACLMessage> e2 = responses.elements();
        while (e2.hasMoreElements()) {
            ACLMessage msg = e2.nextElement();
            if (msg != bestProposal && msg.getPerformative() == ACLMessage.PROPOSE) {
                ACLMessage reject = msg.createReply();
                reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                acceptances.addElement(reject);
                Logger.info(myAgent.getLocalName(), "Sending REJECT_PROPOSAL to " + msg.getSender().getLocalName());
            }
        }
    }

    protected void handleInform(ACLMessage inform) {
        Logger.info(myAgent.getLocalName(), "Received inform from " + inform.getSender().getLocalName() + ": " + inform.getContent());
    }
}