package com.urlshortener.redirect.exception;

public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String shortKey) {
        super("Short URL not found or expired: " + shortKey);
    }
}
