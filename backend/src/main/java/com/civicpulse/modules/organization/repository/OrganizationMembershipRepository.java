package com.civicpulse.modules.organization.repository;

import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.OrganizationMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    @Query("SELECT m FROM OrganizationMembership m JOIN FETCH m.user WHERE m.organization.id = :orgId")
    Page<OrganizationMembership> findByOrganizationIdWithUser(@Param("orgId") UUID orgId, Pageable pageable);

    @Query("SELECT m FROM OrganizationMembership m JOIN FETCH m.organization WHERE m.user.id = :userId")
    List<OrganizationMembership> findByUserIdWithOrg(@Param("userId") UUID userId);

    long countByOrganizationId(UUID organizationId);

    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
