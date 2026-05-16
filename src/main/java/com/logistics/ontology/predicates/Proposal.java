package com.logistics.ontology.predicates;

import jade.content.Predicate;
import jade.core.AID;
import com.logistics.ontology.concepts.Order;
import java.util.Date;          

public class Proposal implements Predicate {
    private Order order;
    private AID resource;
    private double price;
    private Date estimatedDelivery;   
    private double reliability;

    public Date getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(Date estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public Proposal() {}

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public AID getResource() { return resource; }
    public void setResource(AID resource) { this.resource = resource; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getReliability() { return reliability; }
    public void setReliability(double reliability) { this.reliability = reliability; }
}