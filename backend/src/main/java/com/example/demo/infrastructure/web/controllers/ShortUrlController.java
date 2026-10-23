package com.example.demo.infrastructure.web.controllers;

import com.example.demo.application.service.ShortUrlListResult;
import com.example.demo.application.service.ShortUrlService;
import com.example.demo.domain.model.ShortLink;
import com.example.demo.infrastructure.web.dto.CreateShortUrlRequest;
import com.example.demo.infrastructure.web.dto.ShortUrlResponse;
import com.example.demo.infrastructure.web.dto.ListShortUrlsResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class ShortUrlController {
    private final ShortUrlService service;
    private final String baseUrl;

    public ShortUrlController(ShortUrlService service, @Value("${app.public-base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> createShortLink(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortLink savedLink = service.createShortUrl(request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(savedLink));
    }

    @GetMapping
    public ListShortUrlsResponse listShortLinks() {
        ShortUrlListResult result = service.listShortLinks();
        return new ListShortUrlsResponse(result.links().stream().map(this::toResponse).toList(),
                result.fromCache(), result.countdownEnabled(), result.ttlMillis());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShortLink(@PathVariable Long id) {
        service.deleteShortUrl(id);
        return ResponseEntity.noContent().build();
    }

    private ShortUrlResponse toResponse(ShortLink link) {
        return new ShortUrlResponse(link.id(), link.originalUrl(), baseUrl + "/r/" + link.shortCode());
    }
}
