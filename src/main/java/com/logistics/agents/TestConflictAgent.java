package com.logistics.test;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import com.logistics.ontology.*;
import com.logistics.ontology.predicates.ConflictData;
import java.util.*;

public class TestConflictAgent extends Agent {
    protected void setup() {
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(LogisticsOntology.getInstance());

        addBehaviour(new OneShotBehaviour() {
            public void action() {
                ConflictData cd = new ConflictData();
                cd.setResource(new AID("res1", AID.ISLOCALNAME));
                cd.setOrders(Arrays.asList(new AID("order1", AID.ISLOCALNAME), new AID("order2", AID.ISLOCALNAME)));
                cd.setUtilitiesIfWin(Arrays.asList(100.0, 80.0));
                cd.setUtilitiesIfLose(Arrays.asList(-10.0, -10.0));

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID("manager", AID.ISLOCALNAME));
                msg.setLanguage(new SLCodec().getName());
                msg.setOntology(LogisticsOntology.getInstance().getName());
                try {
                    getContentManager().fillContent(msg, cd);
                    send(msg);
                    System.out.println("Test conflict sent.");
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
}