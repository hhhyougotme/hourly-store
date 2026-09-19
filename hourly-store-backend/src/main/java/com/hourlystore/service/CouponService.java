package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourlystore.entity.Coupon;
import com.hourlystore.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final int TTL_BASE_SEC = 120;
    private static final int TTL_JITTER_SEC = 90;

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public static String allKey(long merchantId) {
        return "hourlystore:cache:coupons:merchant:" + merchantId + ":all";
    }

    public static String activeKey(long merchantId) {
        return "hourlystore:cache:coupons:merchant:" + merchantId + ":active";
    }

    public List<Coupon> listByMerchant(Long merchantId) {
        String key = allKey(merchantId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                if (json.isEmpty()) {
                    return List.of();
                }
                return objectMapper.readValue(json, new TypeReference<List<Coupon>>() {});
            }
        } catch (Exception ignored) {
        }

        List<Coupon> list = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getMerchantId, merchantId)
                        .orderByDesc(Coupon::getId));
        cachePut(key, list);
        return list;
    }

    public List<Coupon> listActiveByMerchant(Long merchantId) {
        String key = activeKey(merchantId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                if (json.isEmpty()) {
                    return List.of();
                }
                return objectMapper.readValue(json, new TypeReference<List<Coupon>>() {});
            }
        } catch (Exception ignored) {
        }

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getMerchantId, merchantId)
                        .le(Coupon::getBeginTime, now)
                        .ge(Coupon::getEndTime, now)
                        .gt(Coupon::getStockRemain, 0)
                        .orderByDesc(Coupon::getId));
        cachePut(key, list);
        return list;
    }

    private void cachePut(String key, List<Coupon> list) {
        try {
            int ttl = TTL_BASE_SEC + ThreadLocalRandom.current().nextInt(TTL_JITTER_SEC + 1);
            String payload = list.isEmpty() ? "" : objectMapper.writeValueAsString(list);
            stringRedisTemplate.opsForValue().set(key, payload, Duration.ofSeconds(ttl));
        } catch (Exception ignored) {
        }
    }

    public void evictMerchantCouponCaches(long merchantId) {
        stringRedisTemplate.delete(List.of(allKey(merchantId), activeKey(merchantId)));
    }

    public Coupon requireById(Long id) {
        Coupon c = couponMapper.selectById(id);
        if (c == null) {
            throw new com.hourlystore.common.BizException("Coupon not found");
        }
        return c;
    }
}
