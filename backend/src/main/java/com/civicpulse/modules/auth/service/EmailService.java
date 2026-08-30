package com.civicpulse.modules.auth.service;

import com.civicpulse.modules.auth.model.OtpPurpose;

public interface EmailService {

    /**
     * Send an HTML and plain-text email to the specified recipient.
     *
     * @param to Recipient email address
     * @param subject Subject line
     * @param htmlContent HTML body
     * @param plainTextContent Plain text fallback
     */
    void sendHtmlEmail(String to, String subject, String htmlContent, String plainTextContent);

    /**
     * Deliver a branded OTP verification code email.
     *
     * @param to Recipient email address
     * @param purpose Security purpose of the OTP
     * @param rawOtp The 8-digit verification code
     * @param ttlSeconds Expiration time in seconds
     */
    void sendOtpEmail(String to, OtpPurpose purpose, String rawOtp, long ttlSeconds);
}
