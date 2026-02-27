package com.assignment.demo.service;

import com.assignment.demo.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRedisService {

    private static final Logger log = LoggerFactory.getLogger(UserRedisService.class);

    public static final String ORDER_COUNT_KEY   = "user:%d:order_count";
    public static final String RECENT_ORDERS_KEY = "user:%d:recent_orders";
    private static final int   RECENT_ORDERS_MAX = 10;
    private static final long  RECENT_ORDERS_TTL_SECONDS = 300L;

    private final RedisTemplate<String, String> redisTemplate;

    // Plain ObjectMapper — no polymorphic typing; used for clean JSON strings in the list
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // In-memory dirty set: userIds whose Redis counter missed at least one increment
    // (e.g. Redis was down during createOrder). These counters must be re-seeded from
    // DB on the next read, regardless of whether the Redis key still exists.
    private final Set<Long> dirtyCounterUserIds = ConcurrentHashMap.newKeySet();

    // In-memory dirty set: userIds whose recent-orders list in Redis is known to be
    // stale — either a push failed during an outage, or an order status was updated.
    // On the next read the list key is deleted and fully re-seeded from DB.
    private final Set<Long> dirtyRecentOrdersUserIds = ConcurrentHashMap.newKeySet();

    // ── Counter Operations ──────────────────────────────────────────────────

    public void incrementOrderCount(Long userId) {
        String key = String.format(ORDER_COUNT_KEY, userId);
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (RuntimeException e) {
            log.warn("Redis INCR error for user order count, userId '{}': {}", userId, e.getMessage());
            // Mark dirty in local memory — always succeeds even when Redis is down.
            // getOrderCount will bypass the stale Redis key and force a DB fallback + re-seed.
            dirtyCounterUserIds.add(userId);
        }
    }

    /**
     * Returns the counter from Redis, or null if the key is absent, Redis is unavailable,
     * or the counter is flagged dirty (missed increments during a Redis outage).
     * Null signals the caller to fall back to the DB count and re-seed Redis.
     */
    public Long getOrderCount(Long userId) {
        // If any increment was missed for this user, the cached value is stale — bypass Redis
        if (dirtyCounterUserIds.contains(userId)) {
            log.warn("Counter for userId '{}' is dirty (missed increments during outage), forcing DB fallback", userId);
            return null;
        }
        try {
            String key = String.format(ORDER_COUNT_KEY, userId);
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;
            return Long.parseLong(value);
        } catch (RuntimeException e) {
            log.warn("Redis GET error for user order count, userId '{}': {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Seeds the counter in Redis from a known-accurate DB count and clears the dirty flag.
     * Called after a DB fallback in countUserOrders to restore Redis as the source of truth.
     */
    public void seedOrderCount(Long userId, long count) {
        try {
            String key = String.format(ORDER_COUNT_KEY, userId);
            redisTemplate.opsForValue().set(key, String.valueOf(count));
            // Only clear dirty flag after a successful write — if SET fails, the flag stays
            // and the next read will attempt the DB fallback + re-seed again
            dirtyCounterUserIds.remove(userId);
        } catch (RuntimeException e) {
            log.warn("Redis SET error while seeding order count, userId '{}': {}", userId, e.getMessage());
        }
    }

    // ── Recent Orders List Operations ────────────────────────────────────────

    public void pushRecentOrder(Long userId, OrderResponse orderResponse) {
        try {
            String key = String.format(RECENT_ORDERS_KEY, userId);
            String json = objectMapper.writeValueAsString(orderResponse);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, RECENT_ORDERS_MAX - 1);
            redisTemplate.expire(key, Duration.ofSeconds(RECENT_ORDERS_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.warn("JSON serialization error for recent order, userId '{}': {}", userId, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Redis LPUSH error for recent orders, userId '{}': {}", userId, e.getMessage());
            // Mark dirty — the list in Redis (if it exists) is now missing this order.
            // getRecentOrders will bypass Redis and force a DB fallback + full re-seed.
            dirtyRecentOrdersUserIds.add(userId);
        }
    }

    /**
     * Marks the recent-orders list for a user as dirty so the next read forces a full
     * DB re-seed regardless of whether the Redis key still exists.
     * Called when an order status is updated so the cached snapshot is invalidated.
     */
    public void invalidateRecentOrders(Long userId) {
        dirtyRecentOrdersUserIds.add(userId);
        // Best-effort delete of the Redis key — avoids serving stale data if Redis is up
        try {
            redisTemplate.delete(String.format(RECENT_ORDERS_KEY, userId));
        } catch (RuntimeException e) {
            log.warn("Redis DEL error during recent orders invalidation, userId '{}': {}", userId, e.getMessage());
            // Key not deleted but dirty flag is set, so the read path will still bypass Redis
        }
    }

    /**
     * Returns the list from Redis, or null if:
     * - the list is flagged dirty (missed push or order update invalidated it)
     * - the key is absent (TTL expired or never seeded)
     * - Redis is unavailable
     * Null signals the caller to fall back to DB and fully re-seed.
     */
    public List<OrderResponse> getRecentOrders(Long userId) {
        // Dirty flag means Redis list is stale — bypass it entirely
        if (dirtyRecentOrdersUserIds.contains(userId)) {
            log.warn("Recent orders for userId '{}' are dirty, forcing DB fallback", userId);
            return null;
        }
        try {
            String key = String.format(RECENT_ORDERS_KEY, userId);
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) return null;
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) return null;
            return jsonList.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, OrderResponse.class);
                        } catch (JsonProcessingException e) {
                            log.warn("JSON deserialization error in recent orders, userId '{}': {}", userId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(o -> o != null)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("Redis LRANGE error for recent orders, userId '{}': {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Fully replaces the recent-orders list in Redis from a DB result and clears the dirty flag.
     * The input list must be sorted newest-first (findTop10ByUserIdOrderByCreatedAtDesc order).
     * Iterates in reverse so leftPush leaves the newest order at index 0.
     * Dirty flag is only cleared after a successful write — if Redis fails, flag stays set
     * and the next read will attempt DB fallback + re-seed again.
     */
    public void repopulateRecentOrders(Long userId, List<OrderResponse> orders) {
        if (orders == null || orders.isEmpty()) {
            // Nothing to write — but still try to clear a stale key and dirty flag
            try {
                redisTemplate.delete(String.format(RECENT_ORDERS_KEY, userId));
                dirtyRecentOrdersUserIds.remove(userId);
            } catch (RuntimeException e) {
                log.warn("Redis DEL error during empty repopulate, userId '{}': {}", userId, e.getMessage());
            }
            return;
        }
        try {
            String key = String.format(RECENT_ORDERS_KEY, userId);
            redisTemplate.delete(key);
            for (int i = orders.size() - 1; i >= 0; i--) {
                String json = objectMapper.writeValueAsString(orders.get(i));
                redisTemplate.opsForList().leftPush(key, json);
            }
            redisTemplate.opsForList().trim(key, 0, RECENT_ORDERS_MAX - 1);
            redisTemplate.expire(key, Duration.ofSeconds(RECENT_ORDERS_TTL_SECONDS));
            // Only clear dirty flag after the full write succeeds
            dirtyRecentOrdersUserIds.remove(userId);
        } catch (JsonProcessingException e) {
            log.warn("JSON serialization error during repopulate recent orders, userId '{}': {}", userId, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Redis repopulate error for recent orders, userId '{}': {}", userId, e.getMessage());
        }
    }
}
