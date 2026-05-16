package com.logistics.ontology.concepts;

import jade.content.Concept;

public class Location implements Concept {
    private String city;
    private double latitude;
    private double longitude;

    public Location() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}