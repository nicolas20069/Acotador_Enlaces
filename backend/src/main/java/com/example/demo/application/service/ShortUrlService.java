package com.example.demo.application.service;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.application.ports.ShortUrlRepository;
import com.example.demo.domain.model.ShortLink;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShortUrlService {
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MIN_SHORT_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortUrlRepository repository;
    private final ShortUrlCache cache;

    public ShortUrlService(ShortUrlRepository repository, ShortUrlCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public ShortLink createShortUrl(String rawUrl) {
        String normalizedUrl = validateOriginalUrl(rawUrl);
        String uniqueCode = generateUniqueCode();

        ShortLink createdLink = repository.save(new ShortLink(0L, normalizedUrl, uniqueCode, Instant.now()));
        cache.invalidate();
        return createdLink;
    }

    public ShortUrlListResult listShortLinks() {
        Optional<java.util.List<ShortLink>> cachedLinks = cache.getAll();
        if (cachedLinks.isPresent()) {
            java.util.List<ShortLink> cached = cachedLinks.get();
            if (cached.isEmpty()) {
                cache.invalidate();
            } else {
                cache.markCacheHit();
                return new ShortUrlListResult(cached, true, cache.hasCacheBeenHit(), cache.remainingMillis());
            }
        }

        java.util.List<ShortLink> links = repository.findAll();
        cache.putAll(links);
        return new ShortUrlListResult(links, false, false, cache.remainingMillis());
    }

    public String resolveRedirectTarget(String shortCode) {
        String normalizedCode = validateShortCode(shortCode);
        return repository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace no encontrado"))
                .originalUrl();
    }

    public ShortUrlCacheStatus cacheStatus() {
        long ttlInMillis = cache.remainingMillis();
        return new ShortUrlCacheStatus(ttlInMillis > 0 && cache.hasCacheBeenHit(), Math.max(ttlInMillis, 0));
    }

    public void deleteShortUrl(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        }

        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace no encontrado");
        }

        cache.invalidate();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = buildCode(MIN_SHORT_CODE_LENGTH);
            if (!repository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible generar un código único");
    }

    private static String validateShortCode(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código requerido");
        }

        String normalizedCode = shortCode.trim();
        if (normalizedCode.length() < MIN_SHORT_CODE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código acortado debe tener al menos 8 caracteres");
        }

        return normalizedCode;
    }

    private static String buildCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(ALLOWED_CHARS.charAt(RANDOM.nextInt(ALLOWED_CHARS.length())));
        }
        return code.toString();
    }

    private static String validateOriginalUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar un enlace");
        }

        String normalizedUrl = rawUrl.trim();
        if (normalizedUrl.length() > 2048) {
            throw new IllegalArgumentException("El enlace es demasiado largo");
        }

        try {
            URI uri = URI.create(normalizedUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            boolean isValidHttpUrl = ("http".equals(scheme) || "https".equals(scheme))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;

            if (!isValidHttpUrl) {
                throw new IllegalArgumentException("Ingresa una URL válida que comience por http:// o https://");
            }
            return normalizedUrl;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Ingresa una URL válida que comience por http:// o https://", ex);
        }
    }
}
