package com.civicpulse.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    @Test
    @DisplayName("Should remove dangerous script tags from text input")
    void testSanitizeScriptTags() {
        String dirty = "Hello volunteers! <script>alert('pwned')</script> Welcome to the event.";
        String clean = inputSanitizer.sanitize(dirty);

        assertThat(clean).isEqualTo("Hello volunteers!  Welcome to the event.");
    }

    @Test
    @DisplayName("Should remove javascript pseudo-protocols and onerror handlers")
    void testSanitizeEventHandlers() {
        String dirty = "<img src=x onerror=alert(1)> and javascript:void(0)";
        String clean = inputSanitizer.sanitize(dirty);

        assertThat(clean).doesNotContain("javascript:");
        assertThat(clean).doesNotContain("onerror=");
    }
}
