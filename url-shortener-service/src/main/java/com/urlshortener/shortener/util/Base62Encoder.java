package com.urlshortener.shortener.util;

import org.springframework.stereotype.Component;

/**
 * Base62 Encoder/Decoder
 *
 * Converts a Snowflake long ID (up to 2^63) to a compact alphanumeric string.
 * Alphabet: 0-9, a-z, A-Z  (62 characters)
 *
 * A typical Snowflake ID (~41 bits of timestamp) encodes to 6-7 Base62 chars,
 * which is the industry standard for short URL keys (bit.ly, tinyurl style).
 *
 * Example:  snowflakeId = 7234567890123456789  →  shortKey = "dU3kR9"
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int MIN_LENGTH = 6;

    /**
     * Encode a positive long (Snowflake ID) to a Base62 string.
     * Left-pads with '0' to ensure at least MIN_LENGTH chars.
     */
    public String encode(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive, got: " + id);

        StringBuilder sb = new StringBuilder();
        long num = id;
        while (num > 0) {
            sb.insert(0, ALPHABET.charAt((int)(num % BASE)));
            num /= BASE;
        }

        // Left-pad to ensure minimum length for consistent short URLs
        while (sb.length() < MIN_LENGTH) {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

}
