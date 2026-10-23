package com.example.demo.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlRequest(@NotBlank(message = "La URL es obligatoria") String url) { }
