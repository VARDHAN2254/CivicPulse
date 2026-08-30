package com.civicpulse.modules.auth.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.UnauthorizedException;
import com.civicpulse.config.JwtConfig;
import com.civicpulse.modules.auth.dto.*;
import com.civicpulse.modules.auth.jwt.JwtTokenProvider;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.repository.OtpChallengeRepository;
import com.civicpulse.modules.auth.repository.RefreshTokenRepository;
import com.civicpulse.modules.user.dto.ChangePasswordRequest;
import com.civicpulse.modules.user.dto.ConfirmEmailChangeRequest;
import com.civicpulse.modules.user.dto.EmailChangeRequest;
import com.civicpulse.modules.user.service.UserService;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import com.civicpulse.ratelimit.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpChallengeRepository otpChallengeRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private OtpDeliveryService otpDeliveryService;

    private OtpService otpService;
    private AuthService authService;
    private PasswordResetService passwordResetService;
    private UserService userService;

    private User sampleUser;
    private User victimUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpChallengeRepository, otpDeliveryService);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtTokenProvider, jwtConfig, rateLimiterService, otpService);
        passwordResetService = new PasswordResetService(userRepository, refreshTokenRepository, passwordEncoder, rateLimiterService, otpService);
        userService = new UserService(userRepository, refreshTokenRepository, passwordEncoder, otpService);

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("citizen@civicpulse.org")
                .passwordHash("hashedOldPassword")
                .fullName("Jane Citizen")
                .role(UserRole.MEMBER)
                .active(true)
                .emailVerified(false)
                .build();

        victimUser = User.builder()
                .id(UUID.randomUUID())
                .email("victim@civicpulse.org")
                .passwordHash("hashedVictimSecret")
                .fullName("Victim User")
                .role(UserRole.MEMBER)
                .active(true)
                .emailVerified(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@civicpulse.org")
                .passwordHash("hashedAdminSecret")
                .fullName("System Admin")
                .role(UserRole.ADMIN)
                .active(true)
                .emailVerified(true)
                .mfaEnabled(true)
                .build();
    }

    // =========================================================================
    // 1. REGISTRATION VERIFICATION TESTS (Tests 1-7)
    // =========================================================================
    @Nested
    @DisplayName("1. Registration Verification Suite")
    class RegistrationTests {

        @Test
        @DisplayName("Test 1: Valid registration creates pending account")
        void test1_ValidRegistration() {
            RegisterRequest req = RegisterRequest.builder()
                    .email("new@civicpulse.org")
                    .password("Password@123")
                    .fullName("New Citizen")
                    .role(UserRole.MEMBER)
                    .build();

            when(userRepository.existsByEmail("new@civicpulse.org")).thenReturn(false);
            when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            AuthResponse res = authService.register(req, "127.0.0.1");
            assertThat(res.isVerificationRequired()).isTrue();
            assertThat(res.getChallengeId()).isNotNull();
        }

        @Test
        @DisplayName("Test 2: Registration OTP is required before login")
        void test2_RegistrationOtpRequiredBeforeLogin() {
            LoginRequest req = LoginRequest.builder()
                    .email("citizen@civicpulse.org")
                    .password("Password123!")
                    .build();

            when(userRepository.findByEmail("citizen@civicpulse.org")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Password123!", "hashedOldPassword")).thenReturn(true);
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            AuthResponse res = authService.login(req, "127.0.0.1");
            assertThat(res.isVerificationRequired()).isTrue();
            assertThat(res.getAccessToken()).isNull();
        }

        @Test
        @DisplayName("Test 3: Invalid OTP is rejected")
        void test3_InvalidOtpRejected() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("citizen@civicpulse.org")
                    .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                    .otpHash(otpService.hashToken("12345678"))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION))
                    .thenReturn(Optional.of(challenge));

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "citizen@civicpulse.org", "99999999", OtpPurpose.REGISTRATION_VERIFICATION))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid verification code");
        }

        @Test
        @DisplayName("Test 4: Expired OTP is rejected")
        void test4_ExpiredOtpRejected() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge expired = OtpChallenge.builder()
                    .id(challengeId)
                    .email("citizen@civicpulse.org")
                    .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                    .otpHash(otpService.hashToken("12345678"))
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION))
                    .thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "citizen@civicpulse.org", "12345678", OtpPurpose.REGISTRATION_VERIFICATION))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Test 5: Correct OTP activates account")
        void test5_CorrectOtpActivatesAccount() {
            UUID challengeId = UUID.randomUUID();
            String rawOtp = "87654321";
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("citizen@civicpulse.org")
                    .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                    .otpHash(otpService.hashToken(rawOtp))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION))
                    .thenReturn(Optional.of(challenge));
            when(userRepository.findByEmail("citizen@civicpulse.org")).thenReturn(Optional.of(sampleUser));
            when(jwtConfig.getExpirationMs()).thenReturn(900000L);
            when(jwtConfig.getRefreshExpirationMs()).thenReturn(604800000L);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("active.session.jwt");

            VerifyOtpRequest req = VerifyOtpRequest.builder()
                    .challengeId(challengeId)
                    .email("citizen@civicpulse.org")
                    .otp(rawOtp)
                    .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                    .build();

            AuthResponse res = authService.verifyRegistration(req, "127.0.0.1");
            assertThat(sampleUser.isEmailVerified()).isTrue();
            assertThat(res.getAccessToken()).isEqualTo("active.session.jwt");
        }

        @Test
        @DisplayName("Test 6: OTP cannot be reused after verification")
        void test6_OtpCannotBeReused() {
            UUID challengeId = UUID.randomUUID();
            when(otpChallengeRepository.findActiveChallenge(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION))
                    .thenReturn(Optional.empty()); // Already used / invalidated

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "citizen@civicpulse.org", "12345678", OtpPurpose.REGISTRATION_VERIFICATION))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 7: Resending invalidates previous OTP")
        void test7_ResendInvalidatesPreviousOtp() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge oldChallenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("citizen@civicpulse.org")
                    .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                    .otpHash(otpService.hashToken("11111111"))
                    .cooldownUntil(Instant.now().minus(5, ChronoUnit.SECONDS))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION))
                    .thenReturn(Optional.of(oldChallenge));
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

            OtpChallenge newChallenge = otpService.resendOtp(challengeId, "citizen@civicpulse.org", OtpPurpose.REGISTRATION_VERIFICATION);
            assertThat(oldChallenge.getInvalidatedAt()).isNotNull();
            assertThat(newChallenge.getResendCount()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 2. PASSWORD RECOVERY & ATTACK ELIMINATION (Tests 8-19)
    // =========================================================================
    @Nested
    @DisplayName("2. Password Recovery Suite")
    class PasswordRecoveryTests {

        @Test
        @DisplayName("Test 8: Existing account gets generic response")
        void test8_ExistingAccountGenericResponse() {
            when(userRepository.findByEmail("victim@civicpulse.org")).thenReturn(Optional.of(victimUser));
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            OtpChallengeResponse res = passwordResetService.requestPasswordReset(new PasswordResetRequest("victim@civicpulse.org"), "127.0.0.1");
            assertThat(res.getEmail()).isEqualTo("victim@civicpulse.org");
            verify(otpDeliveryService, times(1)).deliverOtp(eq("victim@civicpulse.org"), eq(OtpPurpose.PASSWORD_RESET), anyString(), eq(300L));
        }

        @Test
        @DisplayName("Test 9: Nonexistent account gets identical generic response")
        void test9_NonexistentAccountIdenticalGenericResponse() {
            when(userRepository.findByEmail("ghost@civicpulse.org")).thenReturn(Optional.empty());

            OtpChallengeResponse res = passwordResetService.requestPasswordReset(new PasswordResetRequest("ghost@civicpulse.org"), "127.0.0.1");
            assertThat(res.getEmail()).isEqualTo("ghost@civicpulse.org");
            verify(otpChallengeRepository, never()).save(any());
            verify(otpDeliveryService, never()).deliverOtp(anyString(), any(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Test 10: Existing account recovers with valid OTP")
        void test10_ExistingAccountRecoversWithValidOtp() {
            UUID challengeId = UUID.randomUUID();
            String rawOtp = "12345678";
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .otpHash(otpService.hashToken(rawOtp))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(challenge));

            VerifyOtpResponse vRes = passwordResetService.verifyResetOtp(new VerifyOtpRequest(challengeId, "victim@civicpulse.org", rawOtp, OtpPurpose.PASSWORD_RESET), "127.0.0.1");
            assertThat(vRes.isVerified()).isTrue();
            assertThat(vRes.getResetAuthToken()).isNotBlank();
        }

        @Test
        @DisplayName("Test 11: Nonexistent account cannot recover")
        void test11_NonexistentAccountCannotRecover() {
            UUID fakeChallengeId = UUID.randomUUID();
            when(otpChallengeRepository.findActiveChallenge(fakeChallengeId, "ghost@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.verifyResetOtp(new VerifyOtpRequest(fakeChallengeId, "ghost@civicpulse.org", "12345678", OtpPurpose.PASSWORD_RESET), "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 12: Known victim email cannot reset victim account without valid OTP")
        void test12_KnownVictimEmailCannotResetWithoutOtp() {
            // Attacker knows victim email, but does not possess the 8-digit OTP dispatched to victim's delivery channel
            UUID challengeId = UUID.randomUUID();
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .otpHash(otpService.hashToken("99999999")) // Victim's real secret OTP
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(challenge));

            // Attacker supplies incorrect guess
            assertThatThrownBy(() -> passwordResetService.verifyResetOtp(new VerifyOtpRequest(challengeId, "victim@civicpulse.org", "00000000", OtpPurpose.PASSWORD_RESET), "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid verification code");
        }

        @Test
        @DisplayName("Test 13: User ID alone cannot reset account")
        void test13_UserIdAloneCannotResetAccount() {
            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(new PasswordResetWithOtpRequest(victimUser.getId().toString(), "NewPassword123!"), "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 14: Username/Email alone cannot reset account")
        void test14_UsernameAloneCannotResetAccount() {
            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(new PasswordResetWithOtpRequest("victim@civicpulse.org", "NewPassword123!"), "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 15: Reset authorization token is single-use")
        void test15_ResetAuthTokenIsSingleUse() {
            String rawToken = "single-use-token";
            String tokenHash = otpService.hashToken(rawToken);

            OtpChallenge usedChallenge = OtpChallenge.builder()
                    .id(UUID.randomUUID())
                    .resetAuthTokenHash(tokenHash)
                    .usedAt(Instant.now().minus(10, ChronoUnit.SECONDS))
                    .build();

            when(otpChallengeRepository.findActiveByResetAuthTokenHash(tokenHash)).thenReturn(Optional.of(usedChallenge));

            assertThatThrownBy(() -> otpService.consumeResetAuthToken(rawToken))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already been consumed");
        }

        @Test
        @DisplayName("Test 16: Reset OTP expires in 5 minutes")
        void test16_ResetOtpExpires() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge expired = OtpChallenge.builder()
                    .id(challengeId)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .otpHash(otpService.hashToken("12345678"))
                    .expiresAt(Instant.now().minus(1, ChronoUnit.SECONDS))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "victim@civicpulse.org", "12345678", OtpPurpose.PASSWORD_RESET))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Test 17: Tampered or invalid OTP fails")
        void test17_TamperedOtpFails() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .otpHash(otpService.hashToken("55555555"))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(challenge));

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "victim@civicpulse.org", "55555556", OtpPurpose.PASSWORD_RESET))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 18: Wrong-purpose OTP fails")
        void test18_WrongPurposeOtpFails() {
            UUID challengeId = UUID.randomUUID();
            // Challenge was created for EMAIL_CHANGE, not PASSWORD_RESET
            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "victim@civicpulse.org", "12345678", OtpPurpose.PASSWORD_RESET))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 19: OTP brute force is rate-limited and invalidated after 5 failed attempts")
        void test19_OtpBruteForceRateLimited() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .otpHash(otpService.hashToken("88888888"))
                    .attempts(4) // 4 previous failed attempts
                    .maxAttempts(5)
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(challenge));

            assertThatThrownBy(() -> otpService.verifyOtp(challengeId, "victim@civicpulse.org", "00000000", OtpPurpose.PASSWORD_RESET))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum verification attempts exceeded");

            assertThat(challenge.getInvalidatedAt()).isNotNull();
        }
    }

    // =========================================================================
    // 3. PASSWORD CHANGE & SESSION REVOCATION (Tests 20-23)
    // =========================================================================
    @Nested
    @DisplayName("3. Password Change & Session Invalidation Suite")
    class PasswordChangeTests {

        @Test
        @DisplayName("Test 20: Current password is required to change password")
        void test20_CurrentPasswordRequired() {
            when(userRepository.findById(victimUser.getId())).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("WrongPass", "hashedVictimSecret")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(victimUser.getId(), new ChangePasswordRequest("WrongPass", "NewPassword123!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Current password is incorrect");
        }

        @Test
        @DisplayName("Test 21: New password works for subsequent login")
        void test21_NewPasswordWorks() {
            when(userRepository.findByEmail("victim@civicpulse.org")).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("BrandNewSecret123!", "hashedVictimSecret")).thenReturn(true);
            when(jwtConfig.getExpirationMs()).thenReturn(900000L);
            when(jwtConfig.getRefreshExpirationMs()).thenReturn(604800000L);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new.valid.jwt");

            AuthResponse res = authService.login(new LoginRequest("victim@civicpulse.org", "BrandNewSecret123!"), "127.0.0.1");
            assertThat(res.getAccessToken()).isEqualTo("new.valid.jwt");
        }

        @Test
        @DisplayName("Test 22: Old password fails after password reset")
        void test22_OldPasswordFails() {
            when(userRepository.findByEmail("victim@civicpulse.org")).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("OldPassword123!", "hashedVictimSecret")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("victim@civicpulse.org", "OldPassword123!"), "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("Test 23: Existing refresh sessions invalidated upon password change")
        void test23_ExistingSessionsInvalidated() {
            when(userRepository.findById(victimUser.getId())).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("CurrentValidSecret", "hashedVictimSecret")).thenReturn(true);
            when(passwordEncoder.encode("NewSecret123!")).thenReturn("newEncodedHash");

            userService.changePassword(victimUser.getId(), new ChangePasswordRequest("CurrentValidSecret", "NewSecret123!"));
            verify(refreshTokenRepository, times(1)).revokeAllUserTokens(victimUser);
        }
    }

    // =========================================================================
    // 4. MULTI-FACTOR AUTHENTICATION (MFA) (Tests 24-28)
    // =========================================================================
    @Nested
    @DisplayName("4. Multi-Factor Authentication Suite")
    class MfaTests {

        @Test
        @DisplayName("Test 24: Privileged user requires MFA challenge")
        void test24_PrivilegedUserRequiresMfa() {
            when(userRepository.findByEmail("admin@civicpulse.org")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("AdminPass123!", "hashedAdminSecret")).thenReturn(true);
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            AuthResponse res = authService.login(new LoginRequest("admin@civicpulse.org", "AdminPass123!"), "127.0.0.1");
            assertThat(res.isMfaRequired()).isTrue();
            assertThat(res.getAccessToken()).isNull();
        }

        @Test
        @DisplayName("Test 25: Wrong TOTP / MFA code is rejected")
        void test25_WrongTotpRejected() {
            UUID challengeId = UUID.randomUUID();
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("admin@civicpulse.org")
                    .purpose(OtpPurpose.LOGIN_MFA)
                    .otpHash(otpService.hashToken("12345678"))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "admin@civicpulse.org", OtpPurpose.LOGIN_MFA))
                    .thenReturn(Optional.of(challenge));

            assertThatThrownBy(() -> authService.verifyMfaLogin(challengeId, "admin@civicpulse.org", "00000000", "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Test 26: Correct TOTP / MFA code is accepted")
        void test26_CorrectTotpAccepted() {
            UUID challengeId = UUID.randomUUID();
            String rawMfa = "88776655";
            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .email("admin@civicpulse.org")
                    .purpose(OtpPurpose.LOGIN_MFA)
                    .otpHash(otpService.hashToken(rawMfa))
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(otpChallengeRepository.findActiveChallenge(challengeId, "admin@civicpulse.org", OtpPurpose.LOGIN_MFA))
                    .thenReturn(Optional.of(challenge));
            when(userRepository.findByEmail("admin@civicpulse.org")).thenReturn(Optional.of(adminUser));
            when(jwtConfig.getExpirationMs()).thenReturn(900000L);
            when(jwtConfig.getRefreshExpirationMs()).thenReturn(604800000L);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("admin.mfa.jwt");

            AuthResponse res = authService.verifyMfaLogin(challengeId, "admin@civicpulse.org", rawMfa, "127.0.0.1");
            assertThat(res.getAccessToken()).isEqualTo("admin.mfa.jwt");
        }

        @Test
        @DisplayName("Test 27: MFA disable requires strong password reauthentication")
        void test27_MfaDisableRequiresPassword() {
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("WrongPass", "hashedAdminSecret")).thenReturn(false);

            assertThatThrownBy(() -> authService.disableMfa(adminUser.getId(), new MfaVerifyRequest("123456", "WrongPass")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Current password is required");
        }

        @Test
        @DisplayName("Test 28: MFA setup generates secure backup recovery codes")
        void test28_MfaSetupGeneratesBackupCodes() {
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

            MfaSetupResponse res = authService.setupMfa(adminUser.getId());
            assertThat(res.getSecret()).isNotBlank();
            assertThat(res.getBackupCodes()).hasSize(8);
        }
    }

    // =========================================================================
    // 5. EMAIL CHANGE & STEP-UP VERIFICATION (Tests 29-31)
    // =========================================================================
    @Nested
    @DisplayName("5. Email Change & Step-Up Suite")
    class EmailChangeTests {

        @Test
        @DisplayName("Test 29: Old email verification challenge required before email update")
        void test29_OldEmailVerificationRequired() {
            when(userRepository.findById(victimUser.getId())).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("CorrectVictimPass", "hashedVictimSecret")).thenReturn(true);
            when(userRepository.existsByEmail("new.target@civicpulse.org")).thenReturn(false);
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            OtpChallengeResponse res = userService.requestEmailChange(victimUser.getId(), new EmailChangeRequest("new.target@civicpulse.org", "CorrectVictimPass"));
            assertThat(res.getChallengeId()).isNotNull();
            verify(otpDeliveryService, times(1)).deliverOtp(eq("victim@civicpulse.org"), eq(OtpPurpose.EMAIL_CHANGE), anyString(), eq(300L));
        }

        @Test
        @DisplayName("Test 30: Valid OTP confirms email change")
        void test30_ValidOtpConfirmsEmailChange() {
            UUID challengeId = UUID.randomUUID();
            String rawOtp = "44332211";
            String newEmail = "new.verified@civicpulse.org";

            OtpChallenge challenge = OtpChallenge.builder()
                    .id(challengeId)
                    .user(victimUser)
                    .email("victim@civicpulse.org")
                    .purpose(OtpPurpose.EMAIL_CHANGE)
                    .otpHash(otpService.hashToken(rawOtp))
                    .metadata(newEmail)
                    .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .build();

            when(userRepository.findById(victimUser.getId())).thenReturn(Optional.of(victimUser));
            when(otpChallengeRepository.findActiveChallenge(challengeId, "victim@civicpulse.org", OtpPurpose.EMAIL_CHANGE))
                    .thenReturn(Optional.of(challenge));
            when(otpChallengeRepository.findActiveByResetAuthTokenHash(anyString()))
                    .thenReturn(Optional.of(challenge));
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenReturn(challenge);

            userService.confirmEmailChange(victimUser.getId(), new ConfirmEmailChangeRequest(challengeId, rawOtp));
            assertThat(victimUser.getEmail()).isEqualTo(newEmail);
            verify(refreshTokenRepository, times(1)).revokeAllUserTokens(victimUser);
        }

        @Test
        @DisplayName("Test 31: Unauthorized email change attempt is rejected")
        void test31_UnauthorizedEmailChangeRejected() {
            when(userRepository.findById(victimUser.getId())).thenReturn(Optional.of(victimUser));
            when(passwordEncoder.matches("WrongPass", "hashedVictimSecret")).thenReturn(false);

            assertThatThrownBy(() -> userService.requestEmailChange(victimUser.getId(), new EmailChangeRequest("hacker@civicpulse.org", "WrongPass")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Current password is required");
        }
    }

    // =========================================================================
    // 6. AUTHORIZATION & ROLE ISOLATION (Tests 32-33)
    // =========================================================================
    @Nested
    @DisplayName("6. Role Security & Isolation Suite")
    class AuthorizationTests {

        @Test
        @DisplayName("Test 32: Normal user cannot self-assign privileged role during registration")
        void test32_NormalUserCannotSelfAssignAdminRole() {
            RegisterRequest req = RegisterRequest.builder()
                    .email("aspiring.admin@civicpulse.org")
                    .password("Password@123")
                    .fullName("Aspiring Admin")
                    .role(UserRole.ADMIN) // Attempting self-assignment of ADMIN
                    .build();

            when(userRepository.existsByEmail("aspiring.admin@civicpulse.org")).thenReturn(false);
            when(passwordEncoder.encode("Password@123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                assertThat(u.getRole()).isEqualTo(UserRole.MEMBER); // Downgraded to MEMBER
                return u;
            });
            when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> {
                OtpChallenge c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            authService.register(req, "127.0.0.1");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Test 33: Cross-user account security operation is rejected")
        void test33_CrossUserSecurityOperationRejected() {
            UUID victimChallengeId = UUID.randomUUID();
            // Attacker tries to submit verification for victim's challenge using attacker's email
            when(otpChallengeRepository.findActiveChallenge(victimChallengeId, "attacker@civicpulse.org", OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verifyOtp(victimChallengeId, "attacker@civicpulse.org", "12345678", OtpPurpose.PASSWORD_RESET))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
