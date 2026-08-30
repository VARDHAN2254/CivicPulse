package com.civicpulse.modules.attendance.service;

import com.civicpulse.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;
    private final String testSecret = "my-secure-test-hmac-key-0987654321-1234567890";

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService(testSecret);
    }

    @Test
    @DisplayName("Should successfully generate and validate dynamic QR token")
    void generateAndValidateToken_Success() {
        UUID registrationId = UUID.randomUUID();

        String token = qrCodeService.generateDynamicQrToken(registrationId);

        assertThat(token).isNotBlank();

        UUID extractedId = qrCodeService.validateAndExtractRegistrationId(token);
        assertThat(extractedId).isEqualTo(registrationId);
    }

    @Test
    @DisplayName("Should reject malformed or tampered QR token")
    void validateToken_Tampered_ThrowsBadRequest() {
        UUID registrationId = UUID.randomUUID();
        String validToken = qrCodeService.generateDynamicQrToken(registrationId);

        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "AAAA";

        assertThatThrownBy(() -> qrCodeService.validateAndExtractRegistrationId(tamperedToken))
                .isInstanceOf(BadRequestException.class);
    }
}
