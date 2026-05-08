package com.logistics.ontology.predicates;

import jade.content.Predicate;
import jade.core.AID;

public class ArbitrationResult implements Predicate {
    private AID winner;               
    private AID loser;                 
    private double compensation;       

    public ArbitrationResult() {}

    public AID getWinner() { return winner; }
    public void setWinner(AID winner) { this.winner = winner; }

    public AID getLoser() { return loser; }
    public void setLoser(AID loser) { this.loser = loser; }

    public double getCompensation() { return compensation; }
    public void setCompensation(double compensation) { this.compensation = compensation; }
}