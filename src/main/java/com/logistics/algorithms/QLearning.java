package com.logistics.algorithms;

import java.util.*;


public class QLearning {
    
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_RATE = 0.2;
    private static final int EPISODES = 1000;
    
    
    private Map<String, Map<String, Double>> qTable = new HashMap<>();
    
    
    private double[] currentWeights = {0.3, 0.4, 0.3};
    
     
    private static class State {
        double costWeight;
        double timeWeight;
        double reliabilityWeight;
        
        State(double cost, double time, double reliability) {
            this.costWeight = cost;
            this.timeWeight = time;
            this.reliabilityWeight = reliability;
        }
        
        String getKey() {
            return String.format("%.1f,%.1f,%.1f", costWeight, timeWeight, reliabilityWeight);
        }
    }
    
     
    private static class Action {
        double costDelta;
        double timeDelta;
        double reliabilityDelta;
        
        Action(double cost, double time, double reliability) {
            this.costDelta = cost;
            this.timeDelta = time;
            this.reliabilityDelta = reliability;
        }
        
        String getKey() {
            return String.format("%+.1f,%+.1f,%+.1f", costDelta, timeDelta, reliabilityDelta);
        }
    }
    
     
    public double[] train(List<TrainingExample> trainingData) {
        Random rand = new Random();
        
        for (int episode = 0; episode < EPISODES; episode++) {
            State currentState = new State(currentWeights[0], currentWeights[1], currentWeights[2]);
            
            for (TrainingExample example : trainingData) {
                
                Action action = selectAction(currentState, rand);
                
                
                State nextState = applyAction(currentState, action);
                
                
                double reward = calculateReward(nextState, example);
                
                
                updateQValue(currentState, action, reward, nextState);
                
                currentState = nextState;
            }
            
            
            if (episode % 100 == 0) {
                System.out.println("Episode " + episode + ", Weights: " + 
                    Arrays.toString(currentWeights));
            }
        }
        
        return currentWeights;
    }
    
     
    private Action selectAction(State state, Random rand) {
        if (rand.nextDouble() < EXPLORATION_RATE) {
            
            return generateRandomAction(rand);
        } else {
            
            return getBestAction(state);
        }
    }
    
     
    private Action generateRandomAction(Random rand) {
        double costDelta = (rand.nextDouble() - 0.5) * 0.2;
        double timeDelta = (rand.nextDouble() - 0.5) * 0.2;
        double reliabilityDelta = -costDelta - timeDelta; 
        
        return new Action(costDelta, timeDelta, reliabilityDelta);
    }
    
     
    private Action getBestAction(State state) {
        Map<String, Double> actions = qTable.getOrDefault(state.getKey(), new HashMap<>());
        
        if (actions.isEmpty()) {
            return generateRandomAction(new Random());
        }
        
        String bestActionKey = Collections.max(actions.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
        
        String[] parts = bestActionKey.split(",");
        return new Action(
            Double.parseDouble(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2])
        );
    }
    
     
    private State applyAction(State state, Action action) {
        double newCost = Math.max(0, Math.min(1, state.costWeight + action.costDelta));
        double newTime = Math.max(0, Math.min(1, state.timeWeight + action.timeDelta));
        double newReliability = Math.max(0, Math.min(1, state.reliabilityWeight + action.reliabilityDelta));
        
        
        double sum = newCost + newTime + newReliability;
        newCost /= sum;
        newTime /= sum;
        newReliability /= sum;
        
        return new State(newCost, newTime, newReliability);
    }
    
     
    private double calculateReward(State state, TrainingExample example) {
        
        double score = state.costWeight * example.costScore +
                      state.timeWeight * example.timeScore +
                      state.reliabilityWeight * example.reliabilityScore;
        
        return score;
    }
    
     
    private void updateQValue(State state, Action action, double reward, State nextState) {
        String stateKey = state.getKey();
        String actionKey = action.getKey();
        
        qTable.computeIfAbsent(stateKey, k -> new HashMap<>());
        Map<String, Double> actions = qTable.get(stateKey);
        
        double currentQ = actions.getOrDefault(actionKey, 0.0);
        double maxNextQ = getMaxQValue(nextState);
        
        
        double newQ = currentQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
        actions.put(actionKey, newQ);
        
        
        if (newQ > currentQ) {
            currentWeights[0] = state.costWeight;
            currentWeights[1] = state.timeWeight;
            currentWeights[2] = state.reliabilityWeight;
        }
    }
    
     
    private double getMaxQValue(State state) {
        Map<String, Double> actions = qTable.getOrDefault(state.getKey(), new HashMap<>());
        return actions.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }
    
     
    public double[] getWeights() {
        return currentWeights;
    }
    
     
    public static class TrainingExample {
        double costScore;
        double timeScore;
        double reliabilityScore;
        double actualPerformance;
        
        public TrainingExample(double cost, double time, double reliability, double performance) {
            this.costScore = cost;
            this.timeScore = time;
            this.reliabilityScore = reliability;
            this.actualPerformance = performance;
        }
    }
}