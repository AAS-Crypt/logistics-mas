package com.logistics.ontology;

import jade.content.onto.*;
import jade.content.schema.*;
import com.logistics.ontology.concepts.*;
import com.logistics.ontology.predicates.*;

public class LogisticsOntology extends Ontology implements LogisticsVocabulary {

    public static final String ONTOLOGY_NAME = "LogisticsOntology";
    private static LogisticsOntology instance = new LogisticsOntology();

    public static LogisticsOntology getInstance() {
        return instance;
    }

    private LogisticsOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            
            add(new ConceptSchema(CARGO), Cargo.class);
            add(new ConceptSchema(LOCATION), Location.class);
            add(new ConceptSchema(ORDER), Order.class);
            add(new ConceptSchema(RESOURCE), Resource.class);
            add(new ConceptSchema(EVENT), Event.class);

            
            add(new PredicateSchema(PROPOSAL), Proposal.class);
            add(new PredicateSchema(ALLOCATION), Allocation.class);
            add(new PredicateSchema(CALL_FOR_PROPOSAL), CallForProposal.class);
            add(new PredicateSchema(EVENT_NOTIFICATION), EventNotification.class);
            add(new PredicateSchema(SUBSCRIBE_TO_EVENTS), SubscribeToEvents.class);

            
            ConceptSchema cargoSchema = (ConceptSchema) getSchema(CARGO);
            cargoSchema.add(CARGO_ID, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            cargoSchema.add(TYPE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            cargoSchema.add(WEIGHT, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            cargoSchema.add(VOLUME, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            cargoSchema.add(MAX_TEMPERATURE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));

            
            ConceptSchema locationSchema = (ConceptSchema) getSchema(LOCATION);
            locationSchema.add(CITY, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            locationSchema.add(LATITUDE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            locationSchema.add(LONGITUDE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));

            
            ConceptSchema orderSchema = (ConceptSchema) getSchema(ORDER);
            orderSchema.add(ORDER_ID, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            orderSchema.add(CARGO, (ConceptSchema) getSchema(CARGO));
            orderSchema.add(ORIGIN, (ConceptSchema) getSchema(LOCATION));
            orderSchema.add(DESTINATION, (ConceptSchema) getSchema(LOCATION));
            orderSchema.add(DEADLINE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.DATE)); 
            orderSchema.add(PRIORITY, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.INTEGER));
            orderSchema.add(MAX_BUDGET, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));

            
            ConceptSchema resourceSchema = (ConceptSchema) getSchema(RESOURCE);
            resourceSchema.add(RESOURCE_ID, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            resourceSchema.add(TYPE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            resourceSchema.add(CAPACITY_WEIGHT, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            resourceSchema.add(CAPACITY_VOLUME, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            resourceSchema.add(LOCATION, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING)); 
            resourceSchema.add(COST_PER_KM, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));

            
            ConceptSchema eventSchema = (ConceptSchema) getSchema(EVENT);
            eventSchema.add(EVENT_ID, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            eventSchema.add(TYPE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            eventSchema.add(SEVERITY, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            eventSchema.add(LOCATION, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            eventSchema.add(TIMESTAMP, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.DATE));
            eventSchema.add(DESCRIPTION, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));

            
            PredicateSchema proposalSchema = (PredicateSchema) getSchema(PROPOSAL);
            proposalSchema.add(ORDER, (ConceptSchema) getSchema(ORDER));
            proposalSchema.add(RESOURCE, (ConceptSchema) BasicOntology.getInstance().getSchema(BasicOntology.AID));
            proposalSchema.add(PRICE, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));
            proposalSchema.add(ESTIMATED_DELIVERY, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.DATE)); 
            proposalSchema.add(RELIABILITY, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.FLOAT));

            
            PredicateSchema allocationSchema = (PredicateSchema) getSchema(ALLOCATION);
            allocationSchema.add(ORDER, (ConceptSchema) getSchema(ORDER));
            allocationSchema.add(RESOURCE, (ConceptSchema) BasicOntology.getInstance().getSchema(BasicOntology.AID));
            allocationSchema.add(ALLOCATION_ACCEPTED, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.BOOLEAN));

            
            PredicateSchema cfpSchema = (PredicateSchema) getSchema(CALL_FOR_PROPOSAL);
            cfpSchema.add(ORDER, (ConceptSchema) getSchema(ORDER));

            
            PredicateSchema notifSchema = (PredicateSchema) getSchema(EVENT_NOTIFICATION);
            notifSchema.add(EVENT, (ConceptSchema) getSchema(EVENT));

            
            PredicateSchema subSchema = (PredicateSchema) getSchema(SUBSCRIBE_TO_EVENTS);
            subSchema.add(EVENT_TYPES, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            subSchema.add(LOCATION, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));
            subSchema.add(SUBSCRIBER, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.STRING));

            
            add(new PredicateSchema(CONFLICT_DATA), ConflictData.class);
            PredicateSchema conflictSchema = (PredicateSchema) getSchema(CONFLICT_DATA);
            conflictSchema.add(RESOURCE, (ConceptSchema) BasicOntology.getInstance().getSchema(BasicOntology.AID));
            conflictSchema.add(COMPETING_ORDERS, (TermSchema) BasicOntology.getInstance().getSchema(BasicOntology.SEQUENCE)); 
        } catch (OntologyException e) {
            e.printStackTrace();
        }
    }
}