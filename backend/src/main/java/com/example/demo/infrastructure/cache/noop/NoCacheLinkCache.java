package com.example.demo.infrastructure.cache.noop;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.domain.model.ShortLink;
import java.util.List;
import java.util.Optional;

// Segunda variante de familia para demostrar Abstract Factory con perfiles de Spring.
public class NoCacheLinkCache implements ShortUrlCache {
    @Override public Optional<List<ShortLink>> getAll() { return Optional.empty(); }
    @Override public void putAll(List<ShortLink> links) { }
    @Override public void markCacheHit() { }
    @Override public boolean hasCacheBeenHit() { return false; }
    @Override public long remainingMillis() { return 0; }
    @Override public void invalidate() { }
}
