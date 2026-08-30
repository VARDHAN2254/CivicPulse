package com.civicpulse.modules.organization.service;

import com.civicpulse.modules.organization.dto.CreateOrganizationRequest;
import com.civicpulse.modules.organization.dto.OrganizationDto;
import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.organization.model.OrganizationMembership;
import com.civicpulse.modules.organization.repository.OrganizationMembershipRepository;
import com.civicpulse.modules.organization.repository.OrganizationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private User sampleUser;
    private Organization sampleOrg;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("organizer@civicpulse.org")
                .fullName("Sarah Jenkins")
                .role(UserRole.ORGANIZER)
                .build();

        sampleOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Green Earth Volunteers")
                .slug("green-earth-volunteers")
                .description("Environmental protection drives")
                .verified(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully create organization with slug and set creator as OWNER")
    void createOrganization_Success() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Green Earth Volunteers")
                .description("Environmental protection drives")
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);
        when(membershipRepository.save(any(OrganizationMembership.class))).thenAnswer(i -> i.getArguments()[0]);

        OrganizationDto result = organizationService.createOrganization(sampleUser.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Green Earth Volunteers");
        assertThat(result.getCurrentUserRole()).isEqualTo(OrgRole.OWNER);

        verify(organizationRepository, times(1)).save(any(Organization.class));
        verify(membershipRepository, times(1)).save(any(OrganizationMembership.class));
    }

    @Test
    @DisplayName("Should retrieve organization by slug")
    void getOrganizationBySlug_Success() {
        when(organizationRepository.findBySlug("green-earth-volunteers")).thenReturn(Optional.of(sampleOrg));
        when(membershipRepository.countByOrganizationId(sampleOrg.getId())).thenReturn(5L);

        OrganizationDto result = organizationService.getOrganizationBySlugOrId("green-earth-volunteers", sampleUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Green Earth Volunteers");
        assertThat(result.getMemberCount()).isEqualTo(5L);
    }
}
