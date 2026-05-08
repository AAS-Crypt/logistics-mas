package com.logistics.ontology.predicates;

import jade.content.Predicate;

public class SubscribeToEvents implements Predicate {
    private String eventTypes;      
    private String location;         
    private String subscriber;       

    public SubscribeToEvents() {}

    public String getEventTypes() { return eventTypes; }
    public void setEventTypes(String eventTypes) { this.eventTypes = eventTypes; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSubscriber() { return subscriber; }
    public void setSubscriber(String subscriber) { this.subscriber = subscriber; }
}