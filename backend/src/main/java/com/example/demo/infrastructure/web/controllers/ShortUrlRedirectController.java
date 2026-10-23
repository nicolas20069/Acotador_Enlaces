package com.example.demo.infrastructure.web.controllers;

import com.example.demo.application.service.ShortUrlService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortUrlRedirectController {
    private final ShortUrlService service;
    public ShortUrlRedirectController(ShortUrlService service) { this.service = service; }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(service.resolveRedirectTarget(code)))
                .build();
    }
}
