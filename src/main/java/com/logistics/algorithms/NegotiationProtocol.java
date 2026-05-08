package com.logistics.algorithms;

import java.util.*;


public class NegotiationProtocol {
    
     
    public static class NegotiationRound {
        private int roundNumber;
        private Map<String, Double> proposals; 
        private Map<String, Double> counterProposals;
        private boolean converged;
        
        public NegotiationRound(int roundNumber) {
            this.roundNumber = roundNumber;
            this.proposals = new HashMap<>();
            this.counterProposals = new HashMap<>();
            this.converged = false;
        }
        
        public void addProposal(String agentId, double value) {
            proposals.put(agentId, value);
        }
        
        public void addCounterProposal(String agentId, double value) {
            counterProposals.put(agentId, value);
        }
        
        public boolean isConverged() {
            return converged;
        }
        
        public void setConverged(boolean converged) {
            this.converged = converged;
        }
        
        public Map<String, Double> getProposals() {
            return proposals;
        }
        
        public Map<String, Double> getCounterProposals() {
            return counterProposals;
        }
    }
    
     
    public static class Coalition {
        private Set<String> memberIds;
        private double totalUtility;
        private Map<String, Double> individualUtilities;
        
        public Coalition() {
            this.memberIds = new HashSet<>();
            this.individualUtilities = new HashMap<>();
        }
        
        public void addMember(String agentId, double utility) {
            memberIds.add(agentId);
            individualUtilities.put(agentId, utility);
            totalUtility += utility;
        }
        
        public boolean isMember(String agentId) {
            return memberIds.contains(agentId);
        }
        
        public Set<String> getMembers() {
            return memberIds;
        }
        
        public double getTotalUtility() {
            return totalUtility;
        }
        
        public Map<String, Double> getIndividualUtilities() {
            return individualUtilities;
        }
    }
    
     
    public static List<Coalition> negotiate(List<String> agents, Map<String, Double> initialUtilities, int maxRounds) {
        List<NegotiationRound> rounds = new ArrayList<>();
        Map<String, Double> currentUtilities = new HashMap<>(initialUtilities);
        
        
        NegotiationRound round1 = new NegotiationRound(1);
        for (String agent : agents) {
            round1.addProposal(agent, currentUtilities.get(agent));
        }
        rounds.add(round1);
        
        
        for (int roundNum = 2; roundNum <= maxRounds; roundNum++) {
            NegotiationRound prevRound = rounds.get(roundNum - 2);
            NegotiationRound currentRound = new NegotiationRound(roundNum);
            
            boolean allConverged = true;
            
            for (String agent : agents) {
                double myProposal = prevRound.getProposals().get(agent);
                
                
                double avgOthersProposal = 0;
                int count = 0;
                for (String other : agents) {
                    if (!other.equals(agent)) {
                        avgOthersProposal += prevRound.getProposals().get(other);
                        count++;
                    }
                }
                if (count > 0) {
                    avgOthersProposal /= count;
                }
                
                
                double counterProposal = myProposal * 0.7 + avgOthersProposal * 0.3;
                currentRound.addCounterProposal(agent, counterProposal);
                
                
                if (Math.abs(counterProposal - myProposal) > 0.01) {
                    allConverged = false;
                }
                
                
                currentRound.addProposal(agent, counterProposal);
                currentUtilities.put(agent, counterProposal);
            }
            
            if (allConverged) {
                currentRound.setConverged(true);
                rounds.add(currentRound);
                break;
            }
            
            rounds.add(currentRound);
        }
        
        
        return formCoalitions(agents, currentUtilities);
    }
    
     
    private static List<Coalition> formCoalitions(List<String> agents, Map<String, Double> utilities) {
        List<Coalition> coalitions = new ArrayList<>();
        Set<String> assigned = new HashSet<>();
        
        for (String agent : agents) {
            if (assigned.contains(agent)) continue;
            
            Coalition coalition = new Coalition();
            coalition.addMember(agent, utilities.get(agent));
            assigned.add(agent);
            
            
            double threshold = 0.1;
            for (String other : agents) {
                if (!assigned.contains(other)) {
                    double diff = Math.abs(utilities.get(agent) - utilities.get(other));
                    if (diff <= threshold) {
                        coalition.addMember(other, utilities.get(other));
                        assigned.add(other);
                    }
                }
            }
            
            coalitions.add(coalition);
        }
        
        return coalitions;
    }
    
     
    public static Map<String, Double> calculateFairnessMetrics(Coalition coalition) {
        Map<String, Double> metrics = new HashMap<>();
        
        Map<String, Double> utilities = coalition.getIndividualUtilities();
        double[] values = utilities.values().stream().mapToDouble(Double::doubleValue).toArray();
        
        
        double sum = 0;
        double sumSquared = 0;
        for (double v : values) {
            sum += v;
            sumSquared += v * v;
        }
        double jainsIndex = (sum * sum) / (values.length * sumSquared);
        metrics.put("jains_index", jainsIndex);
        
        
        Arrays.sort(values);
        double gini = 0;
        double cumulative = 0;
        for (int i = 0; i < values.length; i++) {
            cumulative += values[i];
            gini += (2 * (i + 1) - values.length - 1) * values[i];
        }
        gini /= (values.length * sum);
        metrics.put("gini_coefficient", Math.abs(gini));
        
        
        boolean envyFree = true;
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] < values[i + 1] * 0.9) { 
                envyFree = false;
                break;
            }
        }
        metrics.put("envy_free", envyFree ? 1.0 : 0.0);
        
        return metrics;
    }
}