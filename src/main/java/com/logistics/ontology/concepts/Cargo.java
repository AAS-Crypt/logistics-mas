package com.logistics.ontology.concepts;

import jade.content.Concept;

public class Cargo implements Concept {
    private String cargoId;
    private String type;          
    private float weight;          
    private float volume;          
    private float maxTemperature;  

    public Cargo() {}
    
    public String getCargoId() { return cargoId; }
    public void setCargoId(String cargoId) { this.cargoId = cargoId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }

    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }

    public float getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(float maxTemperature) { this.maxTemperature = maxTemperature; }
}