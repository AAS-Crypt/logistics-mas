package com.logistics.ontology.predicates;

import jade.content.Predicate;
import jade.core.AID;
import java.util.List;

public class ConflictData implements Predicate {
    private AID resource;                 
    private List<AID> orders;              
    private List<Double> utilitiesIfWin;   
    private List<Double> utilitiesIfLose;  

    
    public ConflictData() {}

    
    public AID getResource() { return resource; }
    public void setResource(AID resource) { this.resource = resource; }

    public List<AID> getOrders() { return orders; }
    public void setOrders(List<AID> orders) { this.orders = orders; }

    public List<Double> getUtilitiesIfWin() { return utilitiesIfWin; }
    public void setUtilitiesIfWin(List<Double> utilitiesIfWin) { this.utilitiesIfWin = utilitiesIfWin; }

    public List<Double> getUtilitiesIfLose() { return utilitiesIfLose; }
    public void setUtilitiesIfLose(List<Double> utilitiesIfLose) { this.utilitiesIfLose = utilitiesIfLose; }
}