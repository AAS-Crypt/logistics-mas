package com.logistics.agents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import jade.core.AID;
import com.logistics.ontology.predicates.ConflictData;
import java.util.*;

public class CAGATest {

    private AID order1;
    private AID order2;
    private AID resource;

    @BeforeEach
    public void setUp() {
        order1 = new AID("order1");
        order2 = new AID("order2");
        resource = new AID("resource1");
    }

    @Test
    public void testCAGA_NashBargainingSolution() {
        ConflictData conflict = new ConflictData();
        conflict.setResource(resource);
        List<AID> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        conflict.setOrders(orders);
        List<Double> winUtils = new ArrayList<>();
        winUtils.add(100.0);  
        winUtils.add(80.0);   
        conflict.setUtilitiesIfWin(winUtils);
        List<Double> loseUtils = new ArrayList<>();
        loseUtils.add(-10.0); 
        loseUtils.add(-10.0); 
        conflict.setUtilitiesIfLose(loseUtils);

        double u1_win = winUtils.get(0);
        double u1_lose = loseUtils.get(0);
        double u2_win = winUtils.get(1);
        double u2_lose = loseUtils.get(1);
        double gain1 = u1_win - u1_lose; 
        double gain2 = u2_win - u2_lose; 

        AID winner;
        AID loser;
        double surplus;
        if (gain1 >= gain2) {
            winner = orders.get(0);
            loser = orders.get(1);
            surplus = gain1;
        } else {
            winner = orders.get(1);
            loser = orders.get(0);
            surplus = gain2;
        }
        double compensation = surplus / 2; 

        assertEquals(order1, winner, "Order1 should win (higher gain)");
        assertEquals(order2, loser, "Order2 should lose");
        assertEquals(55.0, compensation, 0.001, "Compensation should be 55");
    }

    @Test
    public void testCAGA_EqualGains() {
        ConflictData conflict = new ConflictData();
        conflict.setResource(resource);
        List<AID> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        conflict.setOrders(orders);
        List<Double> winUtils = new ArrayList<>();
        winUtils.add(100.0);
        winUtils.add(100.0);
        conflict.setUtilitiesIfWin(winUtils);
        List<Double> loseUtils = new ArrayList<>();
        loseUtils.add(-10.0);
        loseUtils.add(-10.0);
        conflict.setUtilitiesIfLose(loseUtils);
        double gain1 = 100.0 - (-10.0);
        double gain2 = 100.0 - (-10.0);

        AID winner = (gain1 >= gain2) ? order1 : order2;
        double compensation = gain1 / 2;
        assertEquals(order1, winner, "With equal gains, order1 should win");
        assertEquals(55.0, compensation, 0.001, "Compensation should be 55");
    }

    @Test
    public void testCAGA_NegativeSurplus() {
        ConflictData conflict = new ConflictData();
        conflict.setResource(resource);
        List<AID> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        conflict.setOrders(orders);
        List<Double> winUtils = new ArrayList<>();
        winUtils.add(50.0);
        winUtils.add(30.0);
        conflict.setUtilitiesIfWin(winUtils);
        List<Double> loseUtils = new ArrayList<>();
        loseUtils.add(-20.0);
        loseUtils.add(-40.0);
        conflict.setUtilitiesIfLose(loseUtils);
        double gain1 = 50.0 - (-20.0); 
        double gain2 = 30.0 - (-40.0); 

        AID winner = (gain1 >= gain2) ? order1 : order2;
        double compensation = gain1 / 2;
        assertEquals(order1, winner, "With equal gains, order1 should win");
        assertEquals(35.0, compensation, 0.001, "Compensation should be 35");
    }
}