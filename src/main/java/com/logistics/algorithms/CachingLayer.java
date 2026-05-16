package com.logistics.algorithms;

import java.util.*;

public class CachingLayer {
    
    private static class CacheEntry {
        private Object value;
        private long timestamp;
        private long ttl; 
        private int accessCount;
        
        public CacheEntry(Object value, long ttl) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
            this.accessCount = 0;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttl;
        }
        public void access() {
            accessCount++;
        }
        public Object getValue() {
            return value;
        }
        public int getAccessCount() {
            return accessCount;
        }
    }
    
    private Map<String, CacheEntry> cache;
    private int maxSize;
    private long defaultTTL;
    private int hits;
    private int misses;
    
    public CachingLayer(int maxSize, long defaultTTL) {
        this.cache = new HashMap<>();
        this.maxSize = maxSize;
        this.defaultTTL = defaultTTL;
        this.hits = 0;
        this.misses = 0;
    }
     
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            misses++;
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            misses++;
            return null;
        }
        entry.access();
        hits++;
        return entry.getValue();
    }
    
    public void put(String key, Object value) {
        put(key, value, defaultTTL);
    }
     
    public void put(String key, Object value, long ttl) {
        if (cache.size() >= maxSize) {
            evictLRU();
        }
        cache.put(key, new CacheEntry(value, ttl));
    }
     
    private void evictLRU() {
        String lruKey = null;
        long oldestAccess = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().timestamp < oldestAccess) {
                oldestAccess = entry.getValue().timestamp;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
        }
    }
     
    private void evictLFU() {
        String lfuKey = null;
        int minAccess = Integer.MAX_VALUE;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().getAccessCount() < minAccess) {
                minAccess = entry.getValue().getAccessCount();
                lfuKey = entry.getKey();
            }
        }
        if (lfuKey != null) {
            cache.remove(lfuKey);
        }
    }
    
    public void remove(String key) {
        cache.remove(key);
    }
     
    public void clear() {
        cache.clear();
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", cache.size());
        stats.put("max_size", maxSize);
        stats.put("hits", hits);
        stats.put("misses", misses);
        stats.put("hit_rate", (hits + misses) > 0 ? (double) hits / (hits + misses) : 0);
        int expired = 0;
        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                expired++;
            }
        }
        stats.put("expired_entries", expired);
        return stats;
    }
     
    public boolean containsKey(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }
    
    public Set<String> getKeys() {
        return cache.keySet();
    }
    
    public void clearExpired() {
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            cache.remove(key);
        }
    }
}