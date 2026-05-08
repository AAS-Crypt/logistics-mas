package com.logistics.test;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class HelloAgent extends Agent {

    protected void setup() {
        System.out.println("Hello! Agent " + getLocalName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("Received: " + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    protected void takeDown() {
        System.out.println("Agent " + getLocalName() + " terminating.");
    }
}