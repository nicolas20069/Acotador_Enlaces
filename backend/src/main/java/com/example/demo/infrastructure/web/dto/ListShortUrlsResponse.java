package com.example.demo.infrastructure.web.dto;

import java.util.List;

public record ListShortUrlsResponse(List<ShortUrlResponse> links, boolean fromCache,
                                boolean countdownEnabled, long ttlMillis) { }
