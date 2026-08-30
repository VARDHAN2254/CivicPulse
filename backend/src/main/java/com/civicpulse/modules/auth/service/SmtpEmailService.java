package com.civicpulse.modules.auth.service;

import com.civicpulse.modules.auth.model.OtpPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${civicpulse.mail.from:noreply@civicpulse.org}")
    private String mailFrom;

    @Value("${civicpulse.mail.from-name:CivicPulse Security}")
    private String mailFromName;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent, String plainTextContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainTextContent, htmlContent);

            mailSender.send(message);
            log.info("Successfully dispatched email [{}] to recipient: [PROTECTED]", subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to deliver email [{}] to recipient: [PROTECTED] due to SMTP error: {}", subject, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during email transmission to [PROTECTED]: {}", e.getMessage());
        }
    }

    @Override
    public void sendOtpEmail(String to, OtpPurpose purpose, String rawOtp, long ttlSeconds) {
        String subject = switch (purpose) {
            case REGISTRATION_VERIFICATION -> "CivicPulse — Verify Your Account Email";
            case PASSWORD_RESET -> "CivicPulse — Password Reset Verification Code";
            case LOGIN_MFA -> "CivicPulse — Multi-Factor Authentication Code";
            case EMAIL_CHANGE -> "CivicPulse — Confirm Your Email Address Change";
            case SENSITIVE_ACTION -> "CivicPulse — Step-Up Verification Code";
        };

        String purposeTitle = switch (purpose) {
            case REGISTRATION_VERIFICATION -> "Account Email Verification";
            case PASSWORD_RESET -> "Password Reset Request";
            case LOGIN_MFA -> "Two-Factor Authentication";
            case EMAIL_CHANGE -> "Email Address Modification";
            case SENSITIVE_ACTION -> "Security Action Verification";
        };

        String purposeDescription = switch (purpose) {
            case REGISTRATION_VERIFICATION -> "Thank you for joining CivicPulse! Please use the following 8-digit verification code to activate your account:";
            case PASSWORD_RESET -> "We received a request to reset the password for your CivicPulse account. Use the 8-digit code below to proceed:";
            case LOGIN_MFA -> "A sign-in attempt requires secondary verification. Enter this 8-digit security code to complete your login:";
            case EMAIL_CHANGE -> "A request was made to update the email address linked to your account. Enter this code to confirm:";
            case SENSITIVE_ACTION -> "A sensitive security action was requested on your account. Verify with the code below:";
        };

        long minutes = Math.max(1, ttlSeconds / 60);

        String plainText = String.format(
                "CivicPulse Security Notification\n\n" +
                "%s\n\n" +
                "Your 8-digit verification code is:\n" +
                "----------------------------------------\n" +
                "  %s\n" +
                "----------------------------------------\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "SECURITY WARNING:\n" +
                "• Do NOT share this code with anyone. CivicPulse staff will never ask for your code.\n" +
                "• If you did not make this request, please ignore this email or secure your account immediately.\n\n" +
                "— The CivicPulse Security Team",
                purposeDescription,
                rawOtp,
                minutes
        );

        String html = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>%s</title>" +
                "</head>" +
                "<body style='margin:0;padding:0;background-color:#0f172a;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif;color:#f8fafc;'>" +
                "<table width='100%%' border='0' cellspacing='0' cellpadding='0' style='background-color:#0f172a;padding:40px 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='100%%' max-width='580' style='max-width:580px;background-color:#1e293b;border:1px solid #334155;border-radius:16px;overflow:hidden;box-shadow:0 25px 50px -12px rgba(0,0,0,0.5);'>" +
                "<!-- Header -->" +
                "<tr><td style='padding:32px 40px;background:linear-gradient(135deg,#0284c7 0%%,#2563eb 100%%);text-align:center;'>" +
                "<h1 style='margin:0;font-size:24px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;'>🏛️ CivicPulse</h1>" +
                "<p style='margin:6px 0 0 0;font-size:13px;color:#e0f2fe;font-weight:500;text-transform:uppercase;letter-spacing:1px;'>Community Engagement & Security</p>" +
                "</td></tr>" +
                "<!-- Body -->" +
                "<tr><td style='padding:36px 40px;'>" +
                "<h2 style='margin:0 0 16px 0;font-size:20px;font-weight:700;color:#ffffff;'>%s</h2>" +
                "<p style='margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#94a3b8;'>%s</p>" +
                "<!-- OTP Code Box -->" +
                "<table width='100%%' border='0' cellspacing='0' cellpadding='0' style='margin:28px 0;background-color:#0f172a;border:2px dashed #38bdf8;border-radius:12px;'>" +
                "<tr><td align='center' style='padding:24px;'>" +
                "<span style='font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:36px;font-weight:800;letter-spacing:8px;color:#38bdf8;'>%s</span>" +
                "<p style='margin:8px 0 0 0;font-size:12px;color:#64748b;'>⏱️ Expires in <strong>%d minutes</strong> &bull; Single-use only</p>" +
                "</td></tr></table>" +
                "<div style='background-color:#0284c715;border-left:4px solid #38bdf8;padding:14px 16px;border-radius:4px;margin:24px 0;'>" +
                "<p style='margin:0;font-size:13px;color:#cbd5e1;line-height:1.5;'><strong>Security Note:</strong> CivicPulse staff will never ask for this code. Never share your verification code or password with anyone.</p>" +
                "</div>" +
                "<p style='margin:24px 0 0 0;font-size:13px;color:#64748b;line-height:1.5;'>If you did not request this verification code, no action is required and you can safely ignore this message. Your account remains secure.</p>" +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding:24px 40px;background-color:#0f172a;border-top:1px solid #334155;text-align:center;'>" +
                "<p style='margin:0;font-size:12px;color:#475569;'>&copy; 2026 CivicPulse Platform. All rights reserved.</p>" +
                "</td></tr>" +
                "</table>" +
                "</td></tr></table>" +
                "</body></html>",
                subject,
                purposeTitle,
                purposeDescription,
                rawOtp,
                minutes
        );

        sendHtmlEmail(to, subject, html, plainText);
    }
}
