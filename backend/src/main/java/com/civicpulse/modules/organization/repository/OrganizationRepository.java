package com.civicpulse.modules.organization.repository;

import com.civicpulse.modules.organization.model.Organization;
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
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Page<Organization> findByVerifiedTrue(Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Organization> searchOrganizations(@Param("query") String query, Pageable pageable);

    @Query("SELECT o FROM Organization o JOIN OrganizationMembership m ON o.id = m.organization.id WHERE m.user.id = :userId")
    List<Organization> findAllByUserId(@Param("userId") UUID userId);

    long countByVerifiedTrue();
}
