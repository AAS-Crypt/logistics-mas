package com.logistics.ontology;

public interface LogisticsVocabulary {
    
    public static final String CARGO = "cargo";
    public static final String LOCATION = "location";
    public static final String ORDER = "order";
    public static final String RESOURCE = "resource";
    public static final String EVENT = "event";

    
    public static final String PROPOSAL = "proposal";
    public static final String ALLOCATION = "allocation";
    public static final String CALL_FOR_PROPOSAL = "callForProposal";
    public static final String EVENT_NOTIFICATION = "eventNotification";
    public static final String SUBSCRIBE_TO_EVENTS = "subscribeToEvents";

    
    public static final String CARGO_ID = "cargoId";
    public static final String TYPE = "type";
    public static final String WEIGHT = "weight";
    public static final String VOLUME = "volume";
    public static final String MAX_TEMPERATURE = "maxTemperature";

    
    public static final String CITY = "city";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

    
    public static final String ORDER_ID = "orderId";
    public static final String ORIGIN = "origin";
    public static final String DESTINATION = "destination";
    public static final String DEADLINE = "deadline";
    public static final String PRIORITY = "priority";
    public static final String MAX_BUDGET = "maxBudget";

    
    public static final String RESOURCE_ID = "resourceId";
    public static final String CAPACITY_WEIGHT = "capacityWeight";
    public static final String CAPACITY_VOLUME = "capacityVolume";
    public static final String COST_PER_KM = "costPerKm";

    
    public static final String PRICE = "price";
    public static final String ESTIMATED_DELIVERY = "estimatedDelivery";
    public static final String RELIABILITY = "reliability";

    
    public static final String EVENT_ID = "eventId";
    public static final String SEVERITY = "severity";
    public static final String TIMESTAMP = "timestamp";
    public static final String DESCRIPTION = "description";

    
    public static final String EVENT_TYPES = "eventTypes";
    public static final String SUBSCRIBER = "subscriber";

    
    public static final String ALLOCATION_ACCEPTED = "accepted";
    
    public static final String CONFLICT_DATA = "conflictData";
    public static final String COMPETING_ORDERS = "competingOrders";
    public static final String UTILITY_IF_WIN = "utilityIfWin";
    public static final String UTILITY_IF_LOSE = "utilityIfLose";

    
    public static final String ARBITRATION_RESULT = "arbitrationResult";
    public final static String WINNER = "winner";
    public final static String COMPENSATION = "compensation";
}