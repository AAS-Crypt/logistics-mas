package com.logistics.ontology.predicates;

import jade.content.Predicate;
import jade.core.AID;
import com.logistics.ontology.concepts.Order;

public class Allocation implements Predicate {
    private Order order;
    private AID resource;
    private boolean accepted;

    public Allocation() {}

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public AID getResource() { return resource; }
    public void setResource(AID resource) { this.resource = resource; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}