package com.urlshortener.shortener.repository;

import com.urlshortener.shortener.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByLongUrl(String longUrl);

    boolean existsByShortKey(String shortKey);

    /** Bulk delete for efficient cron cleanup */
    @Modifying
    @Query("DELETE FROM Url u WHERE u.expiresAt <= :threshold")
    int deleteExpiredBefore(LocalDateTime threshold);
}
