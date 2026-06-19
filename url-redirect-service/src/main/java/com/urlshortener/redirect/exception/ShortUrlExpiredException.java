package com.urlshortener.redirect.exception;

public class ShortUrlExpiredException extends RuntimeException {
    public ShortUrlExpiredException(String shortKey) {
        super("Short URL has expired: " + shortKey);
    }
}
