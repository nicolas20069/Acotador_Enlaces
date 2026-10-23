package com.example.demo.application.service;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.application.ports.ShortUrlRepository;
import com.example.demo.domain.model.ShortLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkServiceTest {
    private ShortUrlRepository repository;
    private ShortUrlCache cache;
    private ShortUrlService service;
    private final ShortLink sample = new ShortLink(1, "https://www.youtube.com/", "ABC123xy", Instant.now());

    @BeforeEach
    void setup() {
        repository = mock(ShortUrlRepository.class);
        cache = mock(ShortUrlCache.class);
        service = new ShortUrlService(repository, cache);
    }

    @Test
    void firstConsultReadsMongoAndStoresCacheOnce() {
        when(cache.getAll()).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(sample));
        when(cache.remainingMillis()).thenReturn(30000L);

        ShortUrlListResult result = service.listShortLinks();
        assertFalse(result.fromCache());
        assertFalse(result.countdownEnabled());
        assertEquals(1, result.links().size());
        verify(repository).findAll();
        verify(cache).putAll(List.of(sample));
        verify(cache, never()).markCacheHit();
    }

    @Test
    void secondConsultReadsRedisWithoutResettingTtl() {
        when(cache.getAll()).thenReturn(Optional.empty(), Optional.of(List.of(sample)));
        when(repository.findAll()).thenReturn(List.of(sample));
        when(cache.hasCacheBeenHit()).thenReturn(true);
        when(cache.remainingMillis()).thenReturn(30000L, 20000L);

        service.listShortLinks();
        ShortUrlListResult second = service.listShortLinks();

        assertTrue(second.fromCache());
        assertTrue(second.countdownEnabled());
        assertEquals(20000, second.ttlMillis());
        verify(cache, times(1)).putAll(List.of(sample));
        verify(repository, times(1)).findAll();
        verify(cache, times(1)).markCacheHit();
    }

    @Test
    void newLinkInvalidatesCache() {
        when(repository.existsByCode(anyString())).thenReturn(false);
        when(repository.save(any(ShortLink.class))).thenAnswer(invocation -> {
            ShortLink incoming = invocation.getArgument(0);
            return new ShortLink(2, incoming.originalUrl(), incoming.shortCode(), incoming.createdAt());
        });
        ShortLink result = service.createShortUrl("https://example.com/path");
        assertEquals(2, result.id());
        verify(cache).invalidate();
    }

    @Test
    void emptyCachedListFallsBackToMongoAndStopsCountdown() {
        when(cache.getAll()).thenReturn(Optional.of(List.of()));
        when(repository.findAll()).thenReturn(List.of(sample));
        when(cache.remainingMillis()).thenReturn(0L);

        ShortUrlListResult result = service.listShortLinks();

        assertFalse(result.fromCache());
        assertFalse(result.countdownEnabled());
        assertEquals(1, result.links().size());
        verify(cache).invalidate();
        verify(repository).findAll();
    }

    @Test
    void invalidSchemeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createShortUrl("javascript:alert(1)"));
        verifyNoInteractions(repository);
    }

    @Test
    void redirectReturnsOriginalUrl() {
        when(repository.findByCode("ABC123xy")).thenReturn(Optional.of(sample));
        assertEquals(sample.originalUrl(), service.resolveRedirectTarget("ABC123xy"));
    }
}
