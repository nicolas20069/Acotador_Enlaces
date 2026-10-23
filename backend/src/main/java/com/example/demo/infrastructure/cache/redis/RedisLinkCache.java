package com.example.demo.infrastructure.cache.redis;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.domain.model.ShortLink;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLinkCache implements ShortUrlCache {
    private static final String LIST_KEY = "shortener:links:all";
    private static final String HIT_KEY = "shortener:links:hit";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisLinkCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<List<ShortLink>> getAll() {
        String value = redis.opsForValue().get(LIST_KEY);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(value, new TypeReference<List<ShortLink>>() { }));
        } catch (Exception ex) {
            // Si cambió el formato, descartar y reconstruir desde MongoDB.
            invalidate();
            return Optional.empty();
        }
    }

    @Override
    public void putAll(List<ShortLink> links) {
        try {
            // SET NX: no sobreescribe una lista aún vigente ni reinicia su TTL.
            String json = mapper.writeValueAsString(links);
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LIST_KEY, json, TTL))) {
                redis.delete(HIT_KEY);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el listado de enlaces", ex);
        }
    }

    @Override
    public void markCacheHit() {
        long ttl = remainingMillis();
        if (ttl > 0) {
            // Primera vez que se recupera desde caché. Conserva el tiempo del original.
            redis.opsForValue().setIfAbsent(HIT_KEY, "1", Duration.ofMillis(ttl));
        }
    }

    @Override
    public boolean hasCacheBeenHit() {
        return Boolean.TRUE.equals(redis.hasKey(HIT_KEY)) && remainingMillis() > 0;
    }

    @Override
    public long remainingMillis() {
        Long ttl = redis.getExpire(LIST_KEY, TimeUnit.MILLISECONDS); // PTTL, sin renovar expiración.
        return ttl == null ? 0 : Math.max(0, ttl);
    }

    @Override
    public void invalidate() {
        redis.delete(List.of(LIST_KEY, HIT_KEY));
    }
}
