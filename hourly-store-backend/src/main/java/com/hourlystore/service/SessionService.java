package com.hourlystore.service;

import com.hourlystore.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    /**
     * Creates a session and invalidates any previous session for the same user (concurrent login control).
     */
    public String createSession(Long userId) {
        String userSessionKey = authProperties.getUserSessionPrefix() + userId;
        String oldToken = stringRedisTemplate.opsForValue().get(userSessionKey);
        if (oldToken != null && !oldToken.isBlank()) {
            stringRedisTemplate.delete(authProperties.getRedisKeyPrefix() + oldToken.trim());
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Duration ttl = Duration.ofHours(authProperties.getTokenTtlHours());
        stringRedisTemplate.opsForValue().set(authProperties.getRedisKeyPrefix() + token, String.valueOf(userId), ttl);
        stringRedisTemplate.opsForValue().set(userSessionKey, token, ttl);
        return token;
    }

    public Optional<Long> findUserId(String bearerOrRawToken) {
        if (bearerOrRawToken == null || bearerOrRawToken.isBlank()) {
            return Optional.empty();
        }
        String token = stripBearer(bearerOrRawToken.trim());
        String key = authProperties.getRedisKeyPrefix() + token;
        String v = stringRedisTemplate.opsForValue().get(key);
        if (v == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String stripBearer(String header) {
        String p = authProperties.getTokenPrefix();
        if (p != null && header.regionMatches(true, 0, p.trim(), 0, p.trim().length())) {
            return header.substring(p.trim().length()).trim();
        }
        return header;
    }
}
