package com.urlshortener.shortener.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShortenResponse {
    private String shortKey;
    private String shortUrl;
    private String longUrl;
    private Long   snowflakeId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
