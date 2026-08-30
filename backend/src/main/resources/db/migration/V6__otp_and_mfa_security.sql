-- V6__otp_and_mfa_security.sql
-- CivicPulse OTP-Centered Authentication, Account Recovery, and MFA Security Schema

-- 1. Create OTP Challenges Table
CREATE TABLE IF NOT EXISTS otp_challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    resend_count INT NOT NULL DEFAULT 0,
    max_resends INT NOT NULL DEFAULT 3,
    cooldown_until TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    reset_auth_token_hash VARCHAR(255),
    reset_auth_token_expires_at TIMESTAMP WITH TIME ZONE,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup by challenge id, email, and purpose
CREATE INDEX IF NOT EXISTS idx_otp_challenges_email_purpose ON otp_challenges(email, purpose);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_reset_token ON otp_challenges(reset_auth_token_hash);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_user_id ON otp_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_created_at ON otp_challenges(created_at);

-- 2. Add MFA columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_backup_codes TEXT;

-- 3. Ensure existing sample users are marked email_verified
UPDATE users SET is_email_verified = TRUE WHERE email IN (
    'admin@civicpulse.org',
    'organizer@civicpulse.org',
    'moderator@civicpulse.org',
    'member@civicpulse.org'
);
