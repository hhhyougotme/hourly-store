package com.hourlystore.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Atomic inventory-style counters in Redis using Lua (compare-and-decrement).
 * Mirrors DB stock for flash-sale events and coupon remaining stock; keys are warmed on startup
 * and refreshed lazily when missing.
 */
@Service
@RequiredArgsConstructor
public class AtomicStockRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> DECR_IF_POSITIVE = new DefaultRedisScript<>();

    static {
        DECR_IF_POSITIVE.setResultType(Long.class);
        DECR_IF_POSITIVE.setScriptText(
                """
                local v = redis.call('GET', KEYS[1])
                if v == false then return -1 end
                local n = tonumber(v)
                if n == nil then return -2 end
                if n <= 0 then return 0 end
                redis.call('DECR', KEYS[1])
                return 1
                """);
    }

    public static String flashStockKey(long eventId) {
        return "hourlystore:inv:flash:" + eventId;
    }

    public static String couponStockKey(long couponId) {
        return "hourlystore:inv:coupon:" + couponId;
    }

    public void setFlashStockIfAbsent(long eventId, int stock) {
        String key = flashStockKey(eventId);
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(Math.max(0, stock)));
        if (Boolean.TRUE.equals(ok)) {
            return;
        }
        // refresh if absent failed — key exists
    }

    public void setCouponStockIfAbsent(long couponId, int remain) {
        stringRedisTemplate.opsForValue().setIfAbsent(couponStockKey(couponId), String.valueOf(Math.max(0, remain)));
    }

    public void overwriteFlashStock(long eventId, int stock) {
        stringRedisTemplate.opsForValue().set(flashStockKey(eventId), String.valueOf(Math.max(0, stock)));
    }

    public void overwriteCouponStock(long couponId, int remain) {
        stringRedisTemplate.opsForValue().set(couponStockKey(couponId), String.valueOf(Math.max(0, remain)));
    }

    /** @return 1 if decremented, 0 if already zero, -1 if key missing, -2 if corrupt */
    public long tryDecrementFlash(long eventId) {
        List<String> keys = Collections.singletonList(flashStockKey(eventId));
        Long r = stringRedisTemplate.execute(DECR_IF_POSITIVE, keys);
        return r == null ? -2 : r;
    }

    public long tryDecrementCoupon(long couponId) {
        List<String> keys = Collections.singletonList(couponStockKey(couponId));
        Long r = stringRedisTemplate.execute(DECR_IF_POSITIVE, keys);
        return r == null ? -2 : r;
    }

    public void incrementFlash(long eventId) {
        stringRedisTemplate.opsForValue().increment(flashStockKey(eventId));
    }

    public void incrementCoupon(long couponId) {
        stringRedisTemplate.opsForValue().increment(couponStockKey(couponId));
    }
}
