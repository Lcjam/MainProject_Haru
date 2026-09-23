package com.example.demo.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("redis")
class RedisIntegrationTest {
    @Test
    void lettuceStringTemplateRoundTripsExpiresAndCleansOnlyItsOwnNamespace() {
        String prefix = "haru:r03:" + UUID.randomUUID() + ":";
        String otherPrefix = "haru:r03:" + UUID.randomUUID() + ":";
        List<String> ownedKeys = List.of(prefix + "roundtrip", prefix + "ttl");
        List<String> otherKeys = List.of(otherPrefix + "preserve");
        LettuceConnectionFactory factory = connectionFactory();
        StringRedisTemplate redis = null;
        try {
            redis = new StringRedisTemplate(factory);
            redis.afterPropertiesSet();
            redis.opsForValue().set(prefix + "roundtrip", "value");
            assertEquals("value", redis.opsForValue().get(prefix + "roundtrip"));
            redis.opsForValue().set(prefix + "ttl", "temporary", Duration.ofSeconds(1));
            Long ttl = redis.getExpire(prefix + "ttl");
            assertNotNull(ttl);
            assertTrue(ttl >= 0 && ttl <= 1, "TTL must be bounded by the requested one second");
            assertExpires(redis, prefix + "ttl");
            redis.delete(prefix + "roundtrip");
            assertFalse(Boolean.TRUE.equals(redis.hasKey(prefix + "roundtrip")));
            redis.opsForValue().set(otherPrefix + "preserve", "other");
            assertEquals("other", redis.opsForValue().get(otherPrefix + "preserve"));
            ownedKeys.forEach(redis::delete);
            assertEquals("other", redis.opsForValue().get(otherPrefix + "preserve"));
            otherKeys.forEach(redis::delete);
        } finally {
            try {
                if (redis != null) {
                    StringRedisTemplate activeRedis = redis;
                    ownedKeys.forEach(activeRedis::delete);
                    otherKeys.forEach(activeRedis::delete);
                }
            } finally {
                factory.destroy();
            }
        }
    }

    private static void assertExpires(StringRedisTemplate redis, String key) {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            if (!Boolean.TRUE.equals(redis.hasKey(key))) return;
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for Redis TTL expiry");
            }
        }
        fail("Redis key did not expire within three seconds");
    }

    private static LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(required("HARU_TEST_REDIS_HOST"), Integer.parseInt(required("HARU_TEST_REDIS_PORT")));
        String password = System.getenv("HARU_TEST_REDIS_PASSWORD");
        if (password != null && !password.isEmpty()) configuration.setPassword(password);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be explicitly configured");
        return value;
    }
}
