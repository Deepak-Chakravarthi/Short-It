package com.urlshortener.shortener.mapper;

import com.urlshortener.shortener.model.dto.response.ShortenResponse;
import com.urlshortener.shortener.model.entity.Url;
import org.springframework.stereotype.Component;

import static com.urlshortener.shortener.constants.AppConstants.BASE_URL;

@Component

public final class UrlShortenerMapper {


    public static ShortenResponse toResponse(Url mapping) {
        return ShortenResponse.builder()
                .shortKey(mapping.getShortKey())
                .shortUrl(BASE_URL + "/" + mapping.getShortKey())
                .longUrl(mapping.getLongUrl())
                .snowflakeId(mapping.getId())
                .expiresAt(mapping.getExpiresAt())
                .createdAt(mapping.getCreatedAt())
                .build();
    }

}
