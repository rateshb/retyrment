package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.FamilyMember;
import com.retyrment.model.User;
import com.retyrment.repository.FamilyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMemberController Unit Tests")
class FamilyMemberControllerUnitTest {

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FamilyMemberController familyMemberController;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    @DisplayName("getAllFamilyMembers returns current user's members")
    void getAllFamilyMembers_returnsList() {
        when(familyMemberRepository.findByUserIdOrderByDateOfBirthAsc("user-1"))
                .thenReturn(List.of(FamilyMember.builder().userId("user-1").build()));

        List<FamilyMember> result = familyMemberController.getAllFamilyMembers();

        assertThat(result).hasSize(1);
        verify(familyMemberRepository).findByUserIdOrderByDateOfBirthAsc("user-1");
    }

    @Test
    @DisplayName("getFamilyMember returns when present, throws when missing")
    void getFamilyMember_returnsOrThrows() {
        FamilyMember member = FamilyMember.builder().id("fm1").userId("user-1").build();
        when(familyMemberRepository.findByIdAndUserId("fm1", "user-1"))
                .thenReturn(Optional.of(member));

        FamilyMember found = familyMemberController.getFamilyMember("fm1");
        assertThat(found.getId()).isEqualTo("fm1");

        when(familyMemberRepository.findByIdAndUserId("fm2", "user-1"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> familyMemberController.getFamilyMember("fm2"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getSelf/getSpouse return null when not found")
    void getSelfAndSpouse_nullWhenNotFound() {
        when(familyMemberRepository.findByUserIdAndRelationship("user-1", FamilyMember.Relationship.SELF))
                .thenReturn(Optional.empty());
        when(familyMemberRepository.findByUserIdAndRelationship("user-1", FamilyMember.Relationship.SPOUSE))
                .thenReturn(Optional.empty());

        assertThat(familyMemberController.getSelf()).isNull();
        assertThat(familyMemberController.getSpouse()).isNull();
    }

    @Nested
    @DisplayName("createFamilyMember defaults")
    class CreateDefaults {
        @Test
        @DisplayName("sets dependent based on relationship and defaults isEarning")
        void setsDependentAndIsEarningDefaults() {
            FamilyMember child = FamilyMember.builder()
                    .relationship(FamilyMember.Relationship.CHILD)
                    .isDependent(null)
                    .isEarning(null)
                    .build();

            when(familyMemberRepository.save(any(FamilyMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FamilyMember saved = familyMemberController.createFamilyMember(child);
            assertThat(saved.getUserId()).isEqualTo("user-1");
            assertThat(saved.getIsDependent()).isTrue();
            assertThat(saved.getIsEarning()).isFalse();
        }

        @Test
        @DisplayName("keeps provided dependent and earning values")
        void keepsProvidedValues() {
            FamilyMember self = FamilyMember.builder()
                    .relationship(FamilyMember.Relationship.SELF)
                    .isDependent(false)
                    .isEarning(true)
                    .build();

            when(familyMemberRepository.save(any(FamilyMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FamilyMember saved = familyMemberController.createFamilyMember(self);
            assertThat(saved.getIsDependent()).isFalse();
            assertThat(saved.getIsEarning()).isTrue();
        }
    }

    @Test
    @DisplayName("updateFamilyMember sets id and userId")
    void updateFamilyMember_setsFields() {
        FamilyMember existing = FamilyMember.builder().id("fm1").userId("user-1").build();
        when(familyMemberRepository.findByIdAndUserId("fm1", "user-1"))
                .thenReturn(Optional.of(existing));
        when(familyMemberRepository.save(any(FamilyMember.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FamilyMember update = FamilyMember.builder().name("Updated").build();
        FamilyMember saved = familyMemberController.updateFamilyMember("fm1", update);

        assertThat(saved.getId()).isEqualTo("fm1");
        assertThat(saved.getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("deleteFamilyMember throws when missing")
    void deleteFamilyMember_throwsWhenMissing() {
        when(familyMemberRepository.existsByIdAndUserId("fm1", "user-1")).thenReturn(false);
        assertThatThrownBy(() -> familyMemberController.deleteFamilyMember("fm1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getFamilySummary aggregates counts and coverage")
    void getFamilySummary_counts() {
        FamilyMember spouse = FamilyMember.builder()
                .relationship(FamilyMember.Relationship.SPOUSE)
                .isEarning(true)
                .isDependent(false)
                .existingHealthCover(200000.0)
                .existingLifeCover(500000.0)
                .dateOfBirth(LocalDate.now().minusYears(35))
                .build();
        FamilyMember child = FamilyMember.builder()
                .relationship(FamilyMember.Relationship.CHILD)
                .isDependent(true)
                .isEarning(false)
                .existingHealthCover(0.0)
                .existingLifeCover(0.0)
                .dateOfBirth(LocalDate.now().minusYears(10))
                .build();
        FamilyMember parent = FamilyMember.builder()
                .relationship(FamilyMember.Relationship.PARENT)
                .isDependent(true)
                .isEarning(false)
                .existingHealthCover(300000.0)
                .existingLifeCover(0.0)
                .dateOfBirth(LocalDate.now().minusYears(65))
                .build();

        when(familyMemberRepository.findByUserId("user-1"))
                .thenReturn(Arrays.asList(spouse, child, parent));

        FamilyMemberController.FamilySummary summary = familyMemberController.getFamilySummary();

        assertThat(summary.getTotalMembers()).isEqualTo(3);
        assertThat(summary.getDependents()).isEqualTo(2);
        assertThat(summary.getEarningMembers()).isEqualTo(1);
        assertThat(summary.getChildren()).isEqualTo(1);
        assertThat(summary.getSeniors()).isEqualTo(1);
        assertThat(summary.getTotalHealthCover()).isEqualTo(500000.0);
        assertThat(summary.getTotalLifeCover()).isEqualTo(500000.0);
    }
}
