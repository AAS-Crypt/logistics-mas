package com.logistics.ontology.predicates;

import jade.content.Predicate;
import com.logistics.ontology.concepts.Order;

public class CallForProposal implements Predicate {
    private Order order;

    public CallForProposal() {}
    public CallForProposal(Order order) {
        this.order = order;
    }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
}