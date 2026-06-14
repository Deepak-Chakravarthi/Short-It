package com.urlshortener.shortener.service;

import com.urlshortener.shortener.model.dto.GeneratedKey;
import com.urlshortener.shortener.model.dto.ShortenRequest;
import com.urlshortener.shortener.model.dto.response.ShortenResponse;
import com.urlshortener.shortener.model.entity.Url;
import com.urlshortener.shortener.repository.UrlRepository;
import com.urlshortener.shortener.util.Base62Encoder;
import com.urlshortener.shortener.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.urlshortener.shortener.mapper.UrlShortenerMapper.toResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Base62Encoder base62Encoder;

    /**
     * Shorten a long URL. Idempotent: same long URL returns same short key.
     */
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        log.info("Shortening URL: {}", request.getLongUrl());

        Optional<Url> existingMapping = findExistingURL(request.getLongUrl());
        if (existingMapping.isPresent()) {
            return toResponse(existingMapping.get());
        }

        GeneratedKey components = generateUniqueShortKey();

        LocalDateTime expiresAt = calculateExpiry(request.getTtlDays());
        Url mapping = buildUrlMapping(request, components, expiresAt);

        urlRepository.save(mapping);
        log.info("URL shortened successfully: {} → {} (snowflakeId={})",
                request.getLongUrl(), components.shortKey(), components.snowflakeId());

        return toResponse(mapping);
    }

    protected Optional<Url> findExistingURL(String longUrl) {
        Optional<Url> existing = urlRepository.findByLongUrl(longUrl);
        existing.ifPresent(mapping ->
                log.info("URL already shortened. Returning existing key: {}", mapping.getShortKey())
        );
        return existing;
    }

    /**
     * Generate Unique Key
     */
    protected GeneratedKey generateUniqueShortKey() {
        long snowflakeId = snowflakeIdGenerator.nextId();
        String shortKey = base62Encoder.encode(snowflakeId);

        // Collision safety-net loop
        if (urlRepository.existsByShortKey(shortKey)) {
            log.warn("Rare collision detected for key={}, regenerating...", shortKey);
            snowflakeId = snowflakeIdGenerator.nextId();
            shortKey = base62Encoder.encode(snowflakeId);
        }

        return new GeneratedKey(snowflakeId, shortKey);
    }

    /**
     * Calculate Expiry
     */

    protected LocalDateTime calculateExpiry(Integer ttlDays) {
        // Fallback to default 60 days if the provided value is null, zero, or negative
        int daysToAdd = (ttlDays != null && ttlDays > 0) ? ttlDays : 60;
        return LocalDateTime.now().plusDays(daysToAdd);
    }

    /**
     * Map the entity
     */
    protected Url buildUrlMapping(ShortenRequest request, GeneratedKey components, LocalDateTime expiresAt) {
        return Url.builder()
                .id(components.snowflakeId())
                .shortKey(components.shortKey())
                .longUrl(request.getLongUrl())
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();
    }
}
