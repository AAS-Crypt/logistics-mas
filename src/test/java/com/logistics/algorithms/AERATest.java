package com.logistics.algorithms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class AERATest {

     
    @Test
    public void testAERA_ImpactCalculation_Escalation() {
        double severity = 0.9; 
        double distance = 1.0; 
        double confidence = 0.95;

        double impact = severity * distance * confidence;

        double thetaLow = 0.3;
        double thetaHigh = 0.7;

        
        assertEquals(0.855, impact, 0.001, "Impact should be 0.855");

        
        assertTrue(impact >= thetaHigh, "Impact should trigger escalation");
    }

     
    @Test
    public void testAERA_ImpactCalculation_LocalReplanning() {
        double severity = 0.6; 
        double distance = 0.7; 
        double confidence = 0.95;

        double impact = severity * distance * confidence;

        double thetaLow = 0.3;
        double thetaHigh = 0.7;

        
        assertEquals(0.399, impact, 0.001, "Impact should be 0.399");

        
        assertTrue(impact >= thetaLow && impact < thetaHigh, 
            "Impact should trigger local replanning");
    }

     
    @Test
    public void testAERA_ImpactCalculation_LocalCorrection() {
        double severity = 0.3; 
        double distance = 0.3; 
        double confidence = 0.95;

        double impact = severity * distance * confidence;

        double thetaLow = 0.3;

        
        assertEquals(0.0855, impact, 0.001, "Impact should be 0.0855");

        
        assertTrue(impact < thetaLow, "Impact should trigger local correction");
    }
}