package com.civicpulse.common.security;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // Basic XSS defense: strip script tags and javascript: hrefs
        return input
                .replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("(?i)<iframe[^>]*>[\\s\\S]*?</iframe>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)onload=", "")
                .replaceAll("(?i)onerror=", "")
                .replaceAll("(?i)onclick=", "")
                .trim();
    }
}
