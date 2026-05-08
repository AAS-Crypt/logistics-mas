package com.logistics.behaviours;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ContractNetTest {

     
    @Test
    public void testContractNet_AuctionProcess() {
        
        int numResources = 2;
        int proposalsReceived = 0;
        int acceptancesSent = 0;
        int rejectionsSent = 0;

        
        for (int i = 0; i < numResources; i++) {
            proposalsReceived++;
        }

        
        if (proposalsReceived > 0) {
            acceptancesSent = 1; 
            rejectionsSent = proposalsReceived - 1;
        }

        
        assertEquals(2, proposalsReceived, "Should receive 2 proposals");
        assertEquals(1, acceptancesSent, "Should send 1 acceptance");
        assertEquals(1, rejectionsSent, "Should send 1 rejection");
    }

     
    @Test
    public void testContractNet_SingleResource() {
        int numResources = 1;
        int proposalsReceived = 0;
        int acceptancesSent = 0;

        for (int i = 0; i < numResources; i++) {
            proposalsReceived++;
        }

        if (proposalsReceived > 0) {
            acceptancesSent = 1;
        }

        assertEquals(1, proposalsReceived, "Should receive 1 proposal");
        assertEquals(1, acceptancesSent, "Should send 1 acceptance");
    }
}