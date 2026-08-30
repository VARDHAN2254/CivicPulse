package com.civicpulse.modules.auth.repository;

import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    @Query("SELECT c FROM OtpChallenge c WHERE c.id = :id AND LOWER(c.email) = LOWER(:email) AND c.purpose = :purpose AND c.invalidatedAt IS NULL AND c.usedAt IS NULL")
    Optional<OtpChallenge> findActiveChallenge(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("purpose") OtpPurpose purpose
    );

    @Query("SELECT c FROM OtpChallenge c WHERE c.resetAuthTokenHash = :tokenHash AND c.invalidatedAt IS NULL AND c.usedAt IS NULL")
    Optional<OtpChallenge> findActiveByResetAuthTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE OtpChallenge c SET c.invalidatedAt = :now WHERE LOWER(c.email) = LOWER(:email) AND c.purpose = :purpose AND c.invalidatedAt IS NULL AND c.usedAt IS NULL")
    void invalidateActiveChallenges(
            @Param("email") String email,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") Instant now
    );
}
