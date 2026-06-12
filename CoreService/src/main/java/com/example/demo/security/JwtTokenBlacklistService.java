package com.example.demo.security;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class JwtTokenBlacklistService {

    // 토큰 블랙리스트 저장소
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // 주기적으로 만료된 토큰을 정리하기 위한 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        log.info("Initializing JWT token blacklist service");
        // 1시간마다 만료된 토큰 정리
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 0, 1, TimeUnit.HOURS);
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down JWT token blacklist service");
        scheduler.shutdown();
    }

    /**
     * 토큰을 블랙리스트에 추가
     */
    public void addToBlacklist(String token, Long expiry) {
        blacklistedTokens.put(token, expiry);
        log.info("Token added to blacklist. Will expire at: {}",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(expiry), ZoneId.systemDefault()));
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        boolean result = blacklistedTokens.containsKey(token);
        if (result) {
            log.debug("Token found in blacklist");
        }
        return result;
    }

    /**
     * 만료된 토큰을 정리
     */
    private void cleanupExpiredTokens() {
        int sizeBefore = blacklistedTokens.size();
        long currentTimeMillis = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < currentTimeMillis);
        int sizeAfter = blacklistedTokens.size();
        int removed = sizeBefore - sizeAfter;

        log.info("Cleaned up {} expired tokens. Remaining: {}", removed, sizeAfter);
    }

    /**
     * 블랙리스트에 추가된 토큰의 만료 시간을 사람이 읽을 수 있는 형태로 변환
     */
    public LocalDateTime getExpiryDateTime(String token) {
        Long expiryMillis = blacklistedTokens.get(token);
        if (expiryMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(expiryMillis),
                ZoneId.systemDefault()
        );
    }
}

