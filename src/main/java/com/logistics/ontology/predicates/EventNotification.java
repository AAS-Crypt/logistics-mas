package com.logistics.ontology.predicates;

import jade.content.Predicate;
import com.logistics.ontology.concepts.Event;

public class EventNotification implements Predicate {
    private Event event;

    public EventNotification() {}
    public EventNotification(Event event) {
        this.event = event;
    }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
}