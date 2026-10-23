package com.example.demo.application.ports;

import com.example.demo.domain.model.ShortLink;
import java.util.List;
import java.util.Optional;

public interface ShortUrlCache {
    Optional<List<ShortLink>> getAll();
    void putAll(List<ShortLink> links);
    void markCacheHit();
    boolean hasCacheBeenHit();
    long remainingMillis();
    void invalidate();
}
