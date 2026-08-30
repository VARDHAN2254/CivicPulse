package com.civicpulse.modules.attendance.service;

import com.civicpulse.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class QrCodeService {

    private final String hmacSecret;
    private static final long QR_TOKEN_TTL_MS = 120_000; // 120 seconds TTL

    public QrCodeService(@Value("${civicpulse.security.jwt.secret:default-qr-hmac-secret-key-12345678901234567890}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String generateDynamicQrToken(UUID registrationId) {
        long timestamp = Instant.now().toEpochMilli();
        String data = registrationId.toString() + "." + timestamp;
        String signature = sign(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((data + "." + signature).getBytes(StandardCharsets.UTF_8));
    }

    public UUID validateAndExtractRegistrationId(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new BadRequestException("QR token cannot be empty.");
        }

        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(qrToken), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Malformed QR token format.");
        }

        String[] parts = decoded.split("\\.");
        if (parts.length != 3) {
            throw new BadRequestException("Invalid QR token structure.");
        }

        String regIdStr = parts[0];
        String timestampStr = parts[1];
        String signature = parts[2];

        // 1. Verify Timestamp & TTL
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid token timestamp.");
        }

        long currentMillis = Instant.now().toEpochMilli();
        if (currentMillis - timestamp > QR_TOKEN_TTL_MS) {
            log.warn("Dynamic QR token expired: age={}ms, maxTTL={}ms", currentMillis - timestamp, QR_TOKEN_TTL_MS);
            throw new BadRequestException("QR code has expired. Please refresh your dynamic ticket pass.");
        }
        if (timestamp > currentMillis + 30_000) {
            throw new BadRequestException("Token timestamp is in the future.");
        }

        // 2. Verify Cryptographic HMAC Signature
        String expectedData = regIdStr + "." + timestampStr;
        String expectedSignature = sign(expectedData);

        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Invalid HMAC signature on QR token for registration ID: {}", regIdStr);
            throw new BadRequestException("Invalid or tampered QR ticket token.");
        }

        try {
            return UUID.fromString(regIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid registration UUID in token.");
        }
    }

    private String sign(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error computing QR HMAC signature", e);
        }
    }
}
