package com.logistics.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class EventSubscriptionTest {

    @Test
    public void testEventSubscription_SubscribeAndNotify() {
        Map<String, List<String>> subscribers = new HashMap<>();
        String eventType = "TRAFFIC_JAM";
        String subscriber = "resource1";

        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
        assertTrue(subscribers.containsKey(eventType), "Should have subscription for event type");
        assertTrue(subscribers.get(eventType).contains(subscriber), "Should have subscriber");
        List<String> notifiedSubscribers = subscribers.get(eventType);
        assertEquals(1, notifiedSubscribers.size(), "Should have 1 subscriber");
        assertEquals(subscriber, notifiedSubscribers.get(0), "Should notify correct subscriber");
    }

    @Test
    public void testEventSubscription_MultipleSubscribers() {
        Map<String, List<String>> subscribers = new HashMap<>();
        String eventType = "TRAFFIC_JAM";
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add("resource1");
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add("resource2");
        assertEquals(2, subscribers.get(eventType).size(), "Should have 2 subscribers");
    }

    @Test
    public void testEventSubscription_DifferentEventTypes() {
        Map<String, List<String>> subscribers = new HashMap<>();

        subscribers.computeIfAbsent("TRAFFIC_JAM", k -> new ArrayList<>()).add("resource1");
        subscribers.computeIfAbsent("WEATHER_ALERT", k -> new ArrayList<>()).add("resource2");

        assertEquals(1, subscribers.get("TRAFFIC_JAM").size(), "Should have 1 subscriber for TRAFFIC_JAM");
        assertEquals(1, subscribers.get("WEATHER_ALERT").size(), "Should have 1 subscriber for WEATHER_ALERT");
    }
}