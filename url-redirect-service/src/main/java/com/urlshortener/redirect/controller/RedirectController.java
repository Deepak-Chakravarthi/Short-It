package com.urlshortener.redirect.controller;

import com.urlshortener.redirect.service.RedirectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Handles all GET /{shortKey} requests.
 *
 * Returns HTTP 302 (temporary redirect) to preserve ability to update the target URL.
 *
 *
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final RedirectService redirectService;

    /**
     * GET /{shortKey}
     * Main redirect endpoint — the hot path hit by every user clicking a short URL.
     */
    @GetMapping("/{shortKey}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        log.info("Redirect request for shortKey={}", shortKey);

        String longUrl = redirectService.resolveLongUrl(shortKey);

        return ResponseEntity
                .status(HttpStatus.FOUND) // 302 Temporary Redirect
                .location(URI.create(longUrl))
                .build();
    }

    /**
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("url-redirect-service is up");
    }
}
