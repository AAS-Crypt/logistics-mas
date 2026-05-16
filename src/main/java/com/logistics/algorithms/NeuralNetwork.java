package com.logistics.algorithms;

import java.util.*;

public class NeuralNetwork {
    
    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double learningRate = 0.01;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    private double sigmoidDerivative(double x) {
        double s = sigmoid(x);
        return s * (1 - s);
    }
    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        Random rand = new Random();
        weightsInputHidden = new double[inputSize][hiddenSize];
        weightsHiddenOutput = new double[hiddenSize][outputSize];
        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] = rand.nextGaussian() * 0.1;
            }
        }
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenOutput[i][j] = rand.nextGaussian() * 0.1;
            }
            biasHidden[i] = rand.nextGaussian() * 0.1;
        }
        for (int i = 0; i < outputSize; i++) {
            biasOutput[i] = rand.nextGaussian() * 0.1;
        }
    }
     
    public double[] forward(double[] input) {
        double[] hidden = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden[i][j];
            }
            hidden[j] = sigmoid(sum);
        }
        double[] output = new double[outputSize];
        for (int k = 0; k < outputSize; k++) {
            double sum = biasOutput[k];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hidden[j] * weightsHiddenOutput[j][k];
            }
            output[k] = sigmoid(sum);
        }
        return output;
    }
    
    public void train(double[] input, double[] target) {
        double[] hidden = new double[hiddenSize];
        double[] hiddenRaw = new double[hiddenSize]; 
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden[i][j];
            }
            hiddenRaw[j] = sum;
            hidden[j] = sigmoid(sum);
        }
        double[] output = new double[outputSize];
        double[] outputRaw = new double[outputSize];
        for (int k = 0; k < outputSize; k++) {
            double sum = biasOutput[k];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hidden[j] * weightsHiddenOutput[j][k];
            }
            outputRaw[k] = sum;
            output[k] = sigmoid(sum);
        }
        double[] outputError = new double[outputSize];
        for (int k = 0; k < outputSize; k++) {
            outputError[k] = (target[k] - output[k]) * sigmoidDerivative(outputRaw[k]);
        }
        double[] hiddenError = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = 0;
            for (int k = 0; k < outputSize; k++) {
                sum += outputError[k] * weightsHiddenOutput[j][k];
            }
            hiddenError[j] = sum * sigmoidDerivative(hiddenRaw[j]);
        }
        for (int j = 0; j < hiddenSize; j++) {
            for (int k = 0; k < outputSize; k++) {
                weightsHiddenOutput[j][k] += learningRate * outputError[k] * hidden[j];
            }
        }
        for (int k = 0; k < outputSize; k++) {
            biasOutput[k] += learningRate * outputError[k];
        }
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] += learningRate * hiddenError[j] * input[i];
            }
        }
        for (int j = 0; j < hiddenSize; j++) {
            biasHidden[j] += learningRate * hiddenError[j];
        }
    }
    
    public void train(double[][] inputs, double[][] targets, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0;
            for (int i = 0; i < inputs.length; i++) {
                train(inputs[i], targets[i]);
                double[] output = forward(inputs[i]);
                for (int j = 0; j < outputSize; j++) {
                    totalError += Math.pow(targets[i][j] - output[j], 2);
                }
            }
            if (epoch % 100 == 0) {
                System.out.println("Epoch " + epoch + ", Error: " + 
                    (totalError / inputs.length));
            }
        }
    }
     
    public double[] predictDemand(double[] features) {
        return forward(features);
    }
    
    public static TrainingData createTrainingData(double[] historicalDemand, int windowSize) {
        int numSamples = historicalDemand.length - windowSize;
        double[][] inputs = new double[numSamples][windowSize];
        double[][] targets = new double[numSamples][1];
        for (int i = 0; i < numSamples; i++) {
            for (int j = 0; j < windowSize; j++) {
                inputs[i][j] = historicalDemand[i + j] / 100.0; 
            }
            targets[i][0] = historicalDemand[i + windowSize] / 100.0;
        }
        return new TrainingData(inputs, targets);
    }
     
    public static class TrainingData {
        public double[][] inputs;
        public double[][] targets;
        public TrainingData(double[][] inputs, double[][] targets) {
            this.inputs = inputs;
            this.targets = targets;
        }
    }
}