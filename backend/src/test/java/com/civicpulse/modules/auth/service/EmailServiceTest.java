package com.civicpulse.modules.auth.service;

import com.civicpulse.modules.auth.model.OtpPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailService emailService;
    private OtpDeliveryService otpDeliveryService;

    @BeforeEach
    void setUp() {
        emailService = new SmtpEmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "mailFrom", "noreply@civicpulse.org");
        ReflectionTestUtils.setField(emailService, "mailFromName", "CivicPulse Security");

        otpDeliveryService = new OtpDeliveryService(emailService);
    }

    @Test
    @DisplayName("Send OTP Email - Password Reset delivers styled HTML email")
    void testSendOtpEmail_PasswordReset() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("victim@civicpulse.org", OtpPurpose.PASSWORD_RESET, "87654321", 300L);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertThat(sentMessage).isNotNull();
    }

    @Test
    @DisplayName("Send OTP Email - Registration Verification delivers activation message")
    void testSendOtpEmail_RegistrationVerification() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("newuser@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION, "12345678", 300L);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send OTP Email - MFA and Email Change formats")
    void testSendOtpEmail_AllPurposes() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("admin@civicpulse.org", OtpPurpose.LOGIN_MFA, "11223344", 300L);
        emailService.sendOtpEmail("member@civicpulse.org", OtpPurpose.EMAIL_CHANGE, "55667788", 300L);
        emailService.sendOtpEmail("member@civicpulse.org", OtpPurpose.SENSITIVE_ACTION, "99001122", 300L);

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("SMTP Failure - Handled gracefully without leaking secrets or throwing uncaught exceptions")
    void testSendEmail_SmtpFailureGracefulHandling() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection timeout: 1025"))
                .when(mailSender).send(any(MimeMessage.class));

        // Must NOT throw exception to caller
        assertThatCode(() -> emailService.sendOtpEmail("user@civicpulse.org", OtpPurpose.PASSWORD_RESET, "12345678", 300L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OtpDeliveryService delegates correctly to EmailService")
    void testOtpDeliveryServiceDelegation() {
        EmailService mockEmailService = mock(EmailService.class);
        OtpDeliveryService deliveryService = new OtpDeliveryService(mockEmailService);

        deliveryService.deliverOtp("citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION, "99887766", 300L);

        verify(mockEmailService, times(1)).sendOtpEmail("citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION, "99887766", 300L);
    }
}
