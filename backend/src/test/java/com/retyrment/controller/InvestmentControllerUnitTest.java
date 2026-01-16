package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Investment;
import com.retyrment.model.Investment.InvestmentType;
import com.retyrment.model.User;
import com.retyrment.repository.InvestmentRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentController Unit Tests - Data Isolation")
class InvestmentControllerUnitTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InvestmentController investmentController;

    private Investment testInvestment;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testInvestment = Investment.builder()
                .id("inv-1")
                .userId("user-1")
                .name("MF Fund")
                .type(InvestmentType.MUTUAL_FUND)
                .currentValue(100000.0)
                .build();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllInvestments - Data Isolation")
    class GetAllInvestments {
        @Test
        @DisplayName("should return only current user's investments")
        void shouldReturnOnlyCurrentUserInvestments() {
            Investment user1Inv2 = Investment.builder().id("inv-2").userId("user-1").name("Stock").build();
            Investment user2Inv = Investment.builder().id("inv-3").userId("user-2").name("Other").build();
            
            when(investmentRepository.findByUserId("user-1")).thenReturn(Arrays.asList(testInvestment, user1Inv2));

            List<Investment> result = investmentController.getAllInvestments();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(inv -> inv.getUserId().equals("user-1"));
            verify(investmentRepository).findByUserId("user-1");
            verify(investmentRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("getInvestmentsByType - Data Isolation")
    class GetInvestmentsByType {
        @Test
        @DisplayName("should return only current user's investments of specified type")
        void shouldReturnOnlyCurrentUserInvestmentsByType() {
            when(investmentRepository.findByUserIdAndType("user-1", InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(testInvestment));

            List<Investment> result = investmentController.getInvestmentsByType(InvestmentType.MUTUAL_FUND);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
            verify(investmentRepository).findByUserIdAndType("user-1", InvestmentType.MUTUAL_FUND);
        }
    }

    @Nested
    @DisplayName("getInvestmentsWithSIP - Data Isolation")
    class GetInvestmentsWithSIP {
        @Test
        @DisplayName("should return only current user's investments with SIP")
        void shouldReturnOnlyCurrentUserInvestmentsWithSIP() {
            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("user-1", 0.0))
                    .thenReturn(Arrays.asList(testInvestment));

            List<Investment> result = investmentController.getInvestmentsWithSIP();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
            verify(investmentRepository).findByUserIdAndMonthlySipGreaterThan("user-1", 0.0);
        }
    }

    @Nested
    @DisplayName("getInvestmentById - Data Isolation")
    class GetInvestmentById {
        @Test
        @DisplayName("should return investment when it belongs to current user")
        void shouldReturnInvestmentWhenBelongsToUser() {
            when(investmentRepository.findByIdAndUserId("inv-1", "user-1"))
                    .thenReturn(Optional.of(testInvestment));

            Investment result = investmentController.getInvestmentById("inv-1");

            assertThat(result.getId()).isEqualTo("inv-1");
            assertThat(result.getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should throw exception when investment belongs to another user")
        void shouldThrowExceptionWhenInvestmentBelongsToOtherUser() {
            when(investmentRepository.findByIdAndUserId("inv-1", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> investmentController.getInvestmentById("inv-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createInvestment - Data Isolation")
    class CreateInvestment {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Investment newInvestment = Investment.builder()
                    .name("New Fund")
                    .type(InvestmentType.MUTUAL_FUND)
                    .currentValue(50000.0)
                    .build();
            
            when(investmentRepository.save(any(Investment.class))).thenAnswer(inv -> {
                Investment i = inv.getArgument(0);
                i.setId("new-id");
                return i;
            });

            Investment result = investmentController.createInvestment(newInvestment);

            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(investmentRepository).save(argThat(inv -> inv.getUserId().equals("user-1")));
        }
    }

    @Nested
    @DisplayName("updateInvestment - Data Isolation")
    class UpdateInvestment {
        @Test
        @DisplayName("should update investment when it belongs to current user")
        void shouldUpdateInvestmentWhenBelongsToUser() {
            when(investmentRepository.findByIdAndUserId("inv-1", "user-1"))
                    .thenReturn(Optional.of(testInvestment));
            when(investmentRepository.save(any(Investment.class))).thenReturn(testInvestment);

            Investment updated = Investment.builder().name("Updated Fund").build();
            Investment result = investmentController.updateInvestment("inv-1", updated);

            assertThat(result).isNotNull();
            verify(investmentRepository).save(argThat(inv -> 
                inv.getId().equals("inv-1") && inv.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should throw exception when updating other user's investment")
        void shouldThrowExceptionWhenUpdatingOtherUserInvestment() {
            when(investmentRepository.findByIdAndUserId("inv-1", "user-1"))
                    .thenReturn(Optional.empty());

            Investment updated = Investment.builder().name("Updated").build();
            
            assertThatThrownBy(() -> investmentController.updateInvestment("inv-1", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteInvestment - Data Isolation")
    class DeleteInvestment {
        @Test
        @DisplayName("should delete investment when it belongs to current user")
        void shouldDeleteInvestmentWhenBelongsToUser() {
            when(investmentRepository.existsByIdAndUserId("inv-1", "user-1")).thenReturn(true);
            doNothing().when(investmentRepository).deleteById("inv-1");

            investmentController.deleteInvestment("inv-1");

            verify(investmentRepository).existsByIdAndUserId("inv-1", "user-1");
            verify(investmentRepository).deleteById("inv-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's investment")
        void shouldThrowExceptionWhenDeletingOtherUserInvestment() {
            when(investmentRepository.existsByIdAndUserId("inv-1", "user-1")).thenReturn(false);

            assertThatThrownBy(() -> investmentController.deleteInvestment("inv-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
