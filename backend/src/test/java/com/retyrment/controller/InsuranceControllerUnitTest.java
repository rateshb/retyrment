package com.retyrment.controller;

import com.retyrment.model.Insurance;
import com.retyrment.model.Insurance.InsuranceType;
import com.retyrment.model.User;
import com.retyrment.repository.InsuranceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceController Unit Tests - Data Isolation")
class InsuranceControllerUnitTest {

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InsuranceController insuranceController;

    private Insurance testInsurance;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testInsurance = Insurance.builder()
                .id("ins-1")
                .userId("user-1")
                .policyName("LIC Jeevan Anand")
                .type(InsuranceType.TERM_LIFE)
                .sumAssured(5000000.0)
                .annualPremium(15000.0)
                .nextPremiumDate(LocalDate.of(2026, 6, 15))
                .build();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllInsurance - Data Isolation")
    class GetAllInsurance {
        @Test
        @DisplayName("should return only current user's insurance policies")
        void shouldReturnOnlyCurrentUserInsurance() {
            Insurance user1Ins2 = Insurance.builder().id("ins-2").userId("user-1").policyName("Health").build();
            Insurance user2Ins = Insurance.builder().id("ins-3").userId("user-2").policyName("Other").build();
            
            when(insuranceRepository.findByUserId("user-1")).thenReturn(Arrays.asList(testInsurance, user1Ins2));

            List<Insurance> result = insuranceController.getAllInsurance();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(ins -> ins.getUserId().equals("user-1"));
            verify(insuranceRepository).findByUserId("user-1");
        }
    }

    @Nested
    @DisplayName("getInsuranceByType - Data Isolation")
    class GetInsuranceByType {
        @Test
        @DisplayName("should return only current user's insurance of specified type")
        void shouldReturnOnlyCurrentUserInsuranceByType() {
            when(insuranceRepository.findByUserIdAndType("user-1", InsuranceType.TERM_LIFE))
                    .thenReturn(Arrays.asList(testInsurance));

            List<Insurance> result = insuranceController.getInsuranceByType(InsuranceType.TERM_LIFE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
        }
    }

    @Nested
    @DisplayName("getInsuranceById - Data Isolation")
    class GetInsuranceById {
        @Test
        @DisplayName("should return insurance when it belongs to current user")
        void shouldReturnInsuranceWhenBelongsToUser() {
            when(insuranceRepository.findByIdAndUserId("ins-1", "user-1"))
                    .thenReturn(Optional.of(testInsurance));

            ResponseEntity<Insurance> result = insuranceController.getInsuranceById("ins-1");

            assertThat(result.getStatusCode().value()).isEqualTo(200);
            assertThat(result.getBody().getId()).isEqualTo("ins-1");
            assertThat(result.getBody().getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should return 404 when insurance belongs to another user")
        void shouldReturn404WhenInsuranceBelongsToOtherUser() {
            when(insuranceRepository.findByIdAndUserId("ins-1", "user-1"))
                    .thenReturn(Optional.empty());

            ResponseEntity<Insurance> result = insuranceController.getInsuranceById("ins-1");

            assertThat(result.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("createInsurance - Data Isolation")
    class CreateInsurance {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Insurance newInsurance = Insurance.builder()
                    .policyName("New Policy")
                    .type(InsuranceType.HEALTH)
                    .sumAssured(1000000.0)
                    .build();
            
            when(insuranceRepository.save(any(Insurance.class))).thenAnswer(inv -> {
                Insurance i = inv.getArgument(0);
                i.setId("new-id");
                return i;
            });

            Insurance result = insuranceController.createInsurance(newInsurance);

            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(insuranceRepository).save(argThat(ins -> ins.getUserId().equals("user-1")));
        }
    }

    @Nested
    @DisplayName("updateInsurance - Data Isolation")
    class UpdateInsurance {
        @Test
        @DisplayName("should update insurance when it belongs to current user")
        void shouldUpdateInsuranceWhenBelongsToUser() {
            when(insuranceRepository.findByIdAndUserId("ins-1", "user-1"))
                    .thenReturn(Optional.of(testInsurance));
            when(insuranceRepository.save(any(Insurance.class))).thenReturn(testInsurance);

            Insurance updated = Insurance.builder().policyName("Updated Policy").build();
            ResponseEntity<Insurance> result = insuranceController.updateInsurance("ins-1", updated);

            assertThat(result.getStatusCode().value()).isEqualTo(200);
            verify(insuranceRepository).save(argThat(ins -> 
                ins.getId().equals("ins-1") && ins.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should return 404 when updating other user's insurance")
        void shouldReturn404WhenUpdatingOtherUserInsurance() {
            when(insuranceRepository.findByIdAndUserId("ins-1", "user-1"))
                    .thenReturn(Optional.empty());

            Insurance updated = Insurance.builder().policyName("Updated").build();
            ResponseEntity<Insurance> result = insuranceController.updateInsurance("ins-1", updated);

            assertThat(result.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("deleteInsurance - Data Isolation")
    class DeleteInsurance {
        @Test
        @DisplayName("should delete insurance when it belongs to current user")
        void shouldDeleteInsuranceWhenBelongsToUser() {
            when(insuranceRepository.existsByIdAndUserId("ins-1", "user-1")).thenReturn(true);
            doNothing().when(insuranceRepository).deleteById("ins-1");

            ResponseEntity<Void> result = insuranceController.deleteInsurance("ins-1");

            assertThat(result.getStatusCode().value()).isEqualTo(204);
            verify(insuranceRepository).existsByIdAndUserId("ins-1", "user-1");
            verify(insuranceRepository).deleteById("ins-1");
        }

        @Test
        @DisplayName("should return 404 when deleting other user's insurance")
        void shouldReturn404WhenDeletingOtherUserInsurance() {
            when(insuranceRepository.existsByIdAndUserId("ins-1", "user-1")).thenReturn(false);

            ResponseEntity<Void> result = insuranceController.deleteInsurance("ins-1");

            assertThat(result.getStatusCode().value()).isEqualTo(404);
            verify(insuranceRepository, never()).deleteById(anyString());
        }
    }
}
