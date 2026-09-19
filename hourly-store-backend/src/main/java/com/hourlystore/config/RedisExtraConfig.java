package com.hourlystore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisExtraConfig {

    /**
     * Dedicated template for Redis Streams (String field values).
     */
    @Bean
    public RedisTemplate<String, String> streamRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        StringRedisSerializer s = new StringRedisSerializer();
        t.setKeySerializer(s);
        t.setValueSerializer(s);
        t.setHashKeySerializer(s);
        t.setHashValueSerializer(s);
        t.afterPropertiesSet();
        return t;
    }
}
