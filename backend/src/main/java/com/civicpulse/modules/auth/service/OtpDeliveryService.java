package com.civicpulse.modules.auth.service;

import com.civicpulse.modules.auth.model.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpDeliveryService {

    private final EmailService emailService;

    public void deliverOtp(String email, OtpPurpose purpose, String rawOtp, long ttlSeconds) {
        // Send real email via configured SMTP / JavaMailSender
        emailService.sendOtpEmail(email, purpose, rawOtp, ttlSeconds);

        // Secure audit log (Strictly never logs raw OTP value or secret credentials)
        log.info("Dispatched {} OTP email to recipient: [PROTECTED]. Expires in {}s", purpose, ttlSeconds);
    }
}
