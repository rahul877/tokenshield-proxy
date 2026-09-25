package io.tokenshield.proxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private DefaultRedisScript<List> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType(List.class);
    }

    public Mono<RateLimitResult> checkRateLimit(String tenantId, long windowMs, long maxTokens, long estimatedTokens) {
        String key = "tokenshield:rate_limit:" + tenantId;
        String now = String.valueOf(System.currentTimeMillis());
        String requestId = UUID.randomUUID().toString();

        List<String> keys = Collections.singletonList(key);
        List<String> args = List.of(now, String.valueOf(windowMs), String.valueOf(maxTokens), String.valueOf(estimatedTokens), requestId);

        return redisTemplate.execute(rateLimitScript, keys, args)
                .next()
                .map(result -> {
                    boolean allowed = ((Long) result.get(0)) == 1L;
                    long currentUsage = (Long) result.get(1);
                    long remainingTokens = (Long) result.get(2);
                    return new RateLimitResult(allowed, currentUsage, remainingTokens);
                })
                .doOnError(e -> log.error("Rate limiting check failed for tenant {}", tenantId, e));
    }

    public record RateLimitResult(boolean allowed, long currentUsage, long remainingTokens) {}
}