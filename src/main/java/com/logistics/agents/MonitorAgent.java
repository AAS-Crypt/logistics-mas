package com.logistics.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.concepts.Event;
import com.logistics.ontology.predicates.EventNotification;
import com.logistics.ontology.predicates.SubscribeToEvents;
import com.logistics.util.Logger;
import java.util.*;

public class MonitorAgent extends Agent {
    private Map<String, List<AID>> subscribers = new HashMap<>();
    private int eventCounter = 0;

    protected void setup() {
        ContentManager cm = getContentManager();
        cm.registerLanguage(new SLCodec());
        cm.registerOntology(LogisticsOntology.getInstance());
        Logger.info(getLocalName(), "MonitorAgent started.");

        addBehaviour(new TickerBehaviour(this, 15000) {
            protected void onTick() {
                generateEvent();
            }
        });

        addBehaviour(new SubscriptionHandler());
    }

    private void generateEvent() {
        Event event = new Event();
        event.setEventId("EVT" + (++eventCounter));
        event.setType("TRAFFIC_JAM");
        event.setSeverity("MEDIUM");
        event.setLocation("Almaty highway");
        event.setTimestamp(new Date());
        event.setDescription("Accident, 30 min delay");

        List<AID> list = subscribers.get(event.getType());
        if (list != null && !list.isEmpty()) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            for (AID a : list) msg.addReceiver(a);
            msg.setLanguage(new SLCodec().getName());
            msg.setOntology(LogisticsOntology.getInstance().getName());
            try {
                getContentManager().fillContent(msg, new EventNotification(event));
                send(msg);
                Logger.info(getLocalName(), "Sent event " + event.getEventId());
            } catch (Exception e) {
                Logger.error(getLocalName(), "Failed to send event notification", e);
            }
        }
    }

    private class SubscriptionHandler extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.SUBSCRIBE) {
                    try {
                        SubscribeToEvents sub = (SubscribeToEvents) getContentManager().extractContent(msg);
                        String[] types = sub.getEventTypes().split(",");
                        for (String t : types) {
                            t = t.trim();
                            subscribers.computeIfAbsent(t, k -> new ArrayList<>()).add(msg.getSender());
                        }
                        Logger.info(getLocalName(), msg.getSender().getLocalName() + " subscribed to " + sub.getEventTypes());
                    } catch (Exception e) {
                        Logger.error(getLocalName(), "Failed to process subscription", e);
                    }
                }
            } else {
                block();
            }
        }
    }
}