package com.toollix.common.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, ConcurrentHashMap<String, AtomicInteger>> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public boolean allow(String key, int maxRequests, Duration window) {
        if (redisTemplate != null) {
            String bucketKey = "rate-limit:" + key + ":" + (System.currentTimeMillis() / window.toMillis());
            Long count = redisTemplate.opsForValue().increment(bucketKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(bucketKey, window);
            }
            return count == null || count <= maxRequests;
        }

        var bucket = localBuckets.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>());
        long slot = System.currentTimeMillis() / window.toMillis();
        String slotKey = String.valueOf(slot);
        var counter = bucket.computeIfAbsent(slotKey, ignored -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current == 1) {
            bucket.entrySet().removeIf(entry -> Long.parseLong(entry.getKey()) < slot);
        }
        return current <= maxRequests;
    }
}
