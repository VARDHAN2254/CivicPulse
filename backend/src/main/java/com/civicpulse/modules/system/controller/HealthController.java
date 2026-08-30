package com.civicpulse.modules.system.controller;

import com.civicpulse.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "Platform health and version diagnostics")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "CivicPulse API",
                "timestamp", Instant.now().toString(),
                "platform", "CivicPulse — Community Event & Engagement Platform"
        );
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/version")
    @Operation(summary = "API Version and build metadata")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion() {
        Map<String, String> version = Map.of(
                "version", "1.0.0",
                "apiVersion", "v1",
                "buildEnvironment", "production-ready",
                "copyright", "Copyright © 2026 Jai Sai Vardhan Reddy. All rights reserved."
        );
        return ResponseEntity.ok(ApiResponse.success(version));
    }
}
