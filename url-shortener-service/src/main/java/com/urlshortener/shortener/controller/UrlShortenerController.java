package com.urlshortener.shortener.controller;

import com.urlshortener.shortener.model.dto.ShortenRequest;
import com.urlshortener.shortener.model.dto.response.ShortenResponse;
import com.urlshortener.shortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    /**
     * POST /api/v1/urls
     * Accepts a long URL and returns the shortened version.
     */
    @PostMapping
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        log.info("POST /api/v1/urls - longUrl={}", request.getLongUrl());
        ShortenResponse response = urlShortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/urls/health
     * Quick liveness check for this service.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("url-shortener-service is up");
    }
}
