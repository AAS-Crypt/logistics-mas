package com.logistics.ontology.concepts;

import jade.content.Concept;

public class Resource implements Concept {
    private String resourceId;
    private String type;           
    private double capacityWeight;  
    private double capacityVolume;  
    private String location;        
    private double costPerKm;        

    public Resource() {}

    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getCapacityWeight() { return capacityWeight; }
    public void setCapacityWeight(double capacityWeight) { this.capacityWeight = capacityWeight; }

    public double getCapacityVolume() { return capacityVolume; }
    public void setCapacityVolume(double capacityVolume) { this.capacityVolume = capacityVolume; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getCostPerKm() { return costPerKm; }
    public void setCostPerKm(double costPerKm) { this.costPerKm = costPerKm; }
}