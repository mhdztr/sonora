package com.musicplayer.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory cache untuk performance optimization
 */
public class CacheManager<K, V> {

    private static class CacheEntry<V> {
        V value;
        LocalDateTime timestamp;

        CacheEntry(V value) {
            this.value = value;
            this.timestamp = LocalDateTime.now();
        }

        boolean isExpired(int ttlMinutes) {
            return LocalDateTime.now().isAfter(timestamp.plusMinutes(ttlMinutes));
        }
    }

    private final Map<K, CacheEntry<V>> cache;
    private final int maxSize;
    private final int ttlMinutes;

    public CacheManager(int maxSize, int ttlMinutes) {
        this.cache = new HashMap<>();
        this.maxSize = maxSize;
        this.ttlMinutes = ttlMinutes;
    }

    public void put(K key, V value) {
        if (cache.size() >= maxSize) {
            // Simple LRU: remove oldest entry
            K oldestKey = null;
            LocalDateTime oldestTime = LocalDateTime.now();

            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                if (entry.getValue().timestamp.isBefore(oldestTime)) {
                    oldestTime = entry.getValue().timestamp;
                    oldestKey = entry.getKey();
                }
            }

            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }

        cache.put(key, new CacheEntry<>(value));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired(ttlMinutes)) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
