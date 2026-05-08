package com.logistics.agents;

import jade.core.Agent;

public class TestAgent extends Agent {
    protected void setup() {
        System.out.println("TestAgent " + getLocalName() + " is working!");
    }
}