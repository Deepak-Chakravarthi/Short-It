package com.urlshortener.shortener.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL must not be blank")
    @Pattern(
        regexp = "^(https?://).+",
        message = "URL must start with http:// or https://"
    )
    @Size(max = 2048, message = "URL must be 2048 characters or fewer")
    private String longUrl;

    private Integer ttlDays;
}
