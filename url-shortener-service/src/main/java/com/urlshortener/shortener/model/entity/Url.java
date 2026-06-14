package com.urlshortener.shortener.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;


@Entity
@Table(name = "url", indexes = {
    @Index(name = "idx_short_key", columnList = "shortKey", unique = true),
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Url {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "short_key", nullable = false, unique = true, length = 10)
    private String shortKey;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
