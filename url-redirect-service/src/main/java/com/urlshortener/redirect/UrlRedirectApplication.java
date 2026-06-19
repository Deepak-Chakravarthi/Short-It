package com.urlshortener.redirect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UrlRedirectApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlRedirectApplication.class, args);
    }
}
