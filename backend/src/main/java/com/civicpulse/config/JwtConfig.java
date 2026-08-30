package com.civicpulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "civicpulse.security.jwt")
public class JwtConfig {
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long expirationMs = 900000; // 15 minutes
    private long refreshExpirationMs = 604800000; // 7 days
}
