package com.example.demo.infrastructure.web.dto;

public record ShortUrlResponse(long id, String originalUrl, String shortenedUrl) { }
