package com.logistics.analytics;

import java.util.*;


public class Dashboard {
    
     
    public enum WidgetType {
        GAUGE, CHART, TABLE, STATUS, ALERT
    }
    
     
    public static class Widget {
        private String id;
        private String title;
        private WidgetType type;
        private Object data;
        private Map<String, Object> config;
        
        public Widget(String id, String title, WidgetType type) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.config = new HashMap<>();
        }
        
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public WidgetType getType() { return type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(String key, Object value) { config.put(key, value); }
    }
    
     
    public static class Alert {
        private String id;
        private String message;
        private String severity; 
        private long timestamp;
        private boolean acknowledged;
        
        public Alert(String id, String message, String severity) {
            this.id = id;
            this.message = message;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
            this.acknowledged = false;
        }
        
        
        public String getId() { return id; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
        public boolean isAcknowledged() { return acknowledged; }
        public void acknowledge() { this.acknowledged = true; }
    }
    
    private Map<String, Widget> widgets;
    private List<Alert> alerts;
    private Map<String, List<Double>> kpiHistory;
    private long refreshInterval; 
    
    public Dashboard(long refreshInterval) {
        this.widgets = new HashMap<>();
        this.alerts = new ArrayList<>();
        this.kpiHistory = new HashMap<>();
        this.refreshInterval = refreshInterval;
    }
    
     
    public void addWidget(Widget widget) {
        widgets.put(widget.getId(), widget);
    }
    
     
    public void updateWidget(String widgetId, Object data) {
        Widget widget = widgets.get(widgetId);
        if (widget != null) {
            widget.setData(data);
        }
    }
    
     
    public void addAlert(String message, String severity) {
        String alertId = "alert_" + System.currentTimeMillis();
        Alert alert = new Alert(alertId, message, severity);
        alerts.add(alert);
        
        
        if (alerts.size() > 100) {
            alerts.remove(0);
        }
    }
    
     
    public void acknowledgeAlert(String alertId) {
        for (Alert alert : alerts) {
            if (alert.getId().equals(alertId)) {
                alert.acknowledge();
                break;
            }
        }
    }
    
     
    public void updateKPI(String kpiName, double value) {
        kpiHistory.computeIfAbsent(kpiName, k -> new ArrayList<>()).add(value);
        
        
        List<Double> history = kpiHistory.get(kpiName);
        if (history.size() > 100) {
            history.remove(0);
        }
    }
    
     
    public double getCurrentKPI(String kpiName) {
        List<Double> history = kpiHistory.get(kpiName);
        if (history != null && !history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        return 0;
    }
    
     
    public double getKPITrend(String kpiName) {
        List<Double> history = kpiHistory.get(kpiName);
        if (history == null || history.size() < 2) {
            return 0;
        }
        
        
        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += history.get(i);
            sumXY += i * history.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
     
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("widget_count", widgets.size());
        summary.put("alert_count", alerts.size());
        summary.put("unacknowledged_alerts", 
            alerts.stream().filter(a -> !a.isAcknowledged()).count());
        summary.put("kpi_count", kpiHistory.size());
        summary.put("refresh_interval", refreshInterval);
        
        return summary;
    }
    
     
    public Map<String, Widget> getWidgets() {
        return widgets;
    }
    
     
    public List<Alert> getAlerts() {
        return alerts;
    }
    
     
    public List<Alert> getUnacknowledgedAlerts() {
        List<Alert> unacknowledged = new ArrayList<>();
        for (Alert alert : alerts) {
            if (!alert.isAcknowledged()) {
                unacknowledged.add(alert);
            }
        }
        return unacknowledged;
    }
}