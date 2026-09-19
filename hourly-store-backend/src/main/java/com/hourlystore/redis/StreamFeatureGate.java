package com.hourlystore.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Streams (XADD / XREADGROUP / XGROUP) require <strong>Redis 5.0+</strong>.
 * The legacy Windows port (Redis 3.0.x) does not support Streams — set {@code hourlystore.streams.enabled=false}
 * or let bootstrap auto-disable after the first command failure so the app still starts.
 */
@Component
public class StreamFeatureGate {

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public StreamFeatureGate(@Value("${hourlystore.streams.enabled:true}") boolean configuredEnabled) {
        if (!configuredEnabled) {
            enabled.set(false);
        }
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void disable() {
        enabled.set(false);
    }
}
