package com.hourlystore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourlystore.dto.PlaceOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrderResultService {

    public static final String KEY_PREFIX = "hourlystore:order-result:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveSuccess(String correlationId, PlaceOrderResponse response) {
        write(correlationId, response);
    }

    public void saveFailed(String correlationId, String message) {
        write(correlationId, PlaceOrderResponse.failed(message));
    }

    public PlaceOrderResponse poll(String correlationId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            PlaceOrderResponse r = get(correlationId);
            if (r != null) {
                return r;
            }
            Thread.sleep(150);
        }
        return null;
    }

    public PlaceOrderResponse get(String correlationId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + correlationId);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, PlaceOrderResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String correlationId, PlaceOrderResponse response) {
        try {
            stringRedisTemplate.opsForValue().set(
                    KEY_PREFIX + correlationId,
                    objectMapper.writeValueAsString(response),
                    Duration.ofMinutes(10));
        } catch (Exception ignored) {
        }
    }
}
