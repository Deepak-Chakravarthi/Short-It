package com.urlshortener.redirect.repository;

import com.urlshortener.redirect.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    @Query("SELECT u FROM Url u WHERE u.shortKey = :shortKey AND (u.expiresAt IS NULL OR u.expiresAt > CURRENT_TIMESTAMP)")
    Optional<Url> findActiveByShortKey(String shortKey);

}
