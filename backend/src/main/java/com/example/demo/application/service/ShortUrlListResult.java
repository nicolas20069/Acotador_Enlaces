package com.example.demo.application.service;

import com.example.demo.domain.model.ShortLink;
import java.util.List;

public record ShortUrlListResult(List<ShortLink> links, boolean fromCache, boolean countdownEnabled, long ttlMillis) { }
