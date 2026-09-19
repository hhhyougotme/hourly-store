package com.hourlystore.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ensures stream + consumer group exist (MKSTREAM via first XADD). Skipped when {@link StreamFeatureGate} is off.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class OrderStreamBootstrap implements ApplicationRunner {

    private final RedisTemplate<String, String> streamRedisTemplate;
    private final StreamFeatureGate streamFeatureGate;

    @Override
    public void run(ApplicationArguments args) {
        if (!streamFeatureGate.isEnabled()) {
            log.info("Redis Streams disabled (hourlystore.streams.enabled=false).");
            return;
        }
        try {
            streamRedisTemplate.opsForStream().add(OrderStreamConsumer.STREAM_KEY, Map.of("_bootstrap", "1"));
        } catch (Exception ex) {
            log.warn("Redis Streams unavailable (need Redis 5+). Disabling stream features. Detail: {}", ex.getMessage());
            streamFeatureGate.disable();
            return;
        }
        try {
            streamRedisTemplate.opsForStream().createGroup(
                    OrderStreamConsumer.STREAM_KEY,
                    ReadOffset.from("0"),
                    OrderStreamConsumer.CONSUMER_GROUP
            );
            log.info("Redis Stream group ready: {}", OrderStreamConsumer.CONSUMER_GROUP);
        } catch (Exception ex) {
            String m = ex.getMessage() == null ? "" : ex.getMessage();
            if (m.contains("BUSYGROUP") || m.contains("busy")) {
                log.info("Redis Stream group already exists: {}", OrderStreamConsumer.CONSUMER_GROUP);
            } else {
                log.warn("Could not create Redis Stream group. Disabling stream features. Detail: {}", m);
                streamFeatureGate.disable();
            }
        }
    }
}
