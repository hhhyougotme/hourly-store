package com.hourlystore.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStreamPublisher {

    public static final String TYPE_CREATE_ORDER = "CREATE_ORDER";

    private final RedisTemplate<String, String> streamRedisTemplate;
    private final StreamFeatureGate streamFeatureGate;

    public void publishCreateOrder(
            String correlationId,
            long userId,
            long flashSaleEventId,
            long merchantId,
            Long productId,
            BigDecimal amount) {
        if (!streamFeatureGate.isEnabled()) {
            return;
        }
        try {
            Map<String, String> body = new HashMap<>();
            body.put("type", TYPE_CREATE_ORDER);
            body.put("correlationId", correlationId);
            body.put("userId", Long.toString(userId));
            body.put("flashSaleEventId", Long.toString(flashSaleEventId));
            body.put("merchantId", Long.toString(merchantId));
            body.put("productId", productId != null ? Long.toString(productId) : "");
            body.put("amount", amount != null ? amount.toPlainString() : "1");
            streamRedisTemplate.opsForStream().add(OrderStreamConsumer.STREAM_KEY, body);
        } catch (Exception ex) {
            log.warn("Failed to append order event to Redis Stream: {}", ex.getMessage());
            streamFeatureGate.disable();
        }
    }
}
