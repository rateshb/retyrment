package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.model.UserFeatureAccess;
import com.retyrment.repository.ExpenseRepository;
import com.retyrment.repository.FamilyMemberRepository;
import com.retyrment.repository.GoalRepository;
import com.retyrment.repository.IncomeRepository;
import com.retyrment.repository.InsuranceRepository;
import com.retyrment.repository.InvestmentRepository;
import com.retyrment.repository.LoanRepository;
import com.retyrment.repository.UserRepository;
import com.retyrment.service.FeatureAccessService;
import com.retyrment.service.RoleExpiryService;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Unit Tests")
class AdminControllerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleExpiryService roleExpiryService;

    @Mock
    private FeatureAccessService featureAccessService;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        lenient().when(incomeRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(investmentRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(expenseRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(insuranceRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(loanRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(goalRepository.countByUserId(anyString())).thenReturn(0L);
        lenient().when(familyMemberRepository.countByUserId(anyString())).thenReturn(0L);
        
        adminUser = User.builder()
                .id("admin123")
                .email("admin@example.com")
                .name("Admin User")
                .role(User.UserRole.ADMIN)
                .build();

        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .name("Test User")
                .role(User.UserRole.FREE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {
        @Test
        @DisplayName("should return all users when admin")
        void shouldReturnAllUsersWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, adminUser));
            when(roleExpiryService.countExpiringRoles(7)).thenReturn(0L);

            ResponseEntity<?> response = adminController.getAllUsers();

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("total")).isEqualTo(2);
            assertThat(body).containsKey("users");
            assertThat(body).containsKey("stats");
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.getAllUsers();

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should calculate stats correctly")
        void shouldCalculateStatsCorrectly() {
            User freeUser = User.builder().id("free1").role(User.UserRole.FREE).build();
            User proUser = User.builder().id("pro1").role(User.UserRole.PRO).build();
            User adminUser2 = User.builder().id("admin2").role(User.UserRole.ADMIN).build();

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findAll()).thenReturn(Arrays.asList(freeUser, proUser, adminUser2));
            when(roleExpiryService.countExpiringRoles(7)).thenReturn(0L);

            ResponseEntity<?> response = adminController.getAllUsers();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) body.get("stats");
            assertThat(stats.get("free")).isEqualTo(1L);
            assertThat(stats.get("pro")).isEqualTo(1L);
            assertThat(stats.get("admin")).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("updateUserRole")
    class UpdateUserRole {
        @Test
        @DisplayName("should update user role to PRO with duration")
        void shouldUpdateUserRoleToProWithDuration() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "PRO");
            request.put("durationDays", 30);
            request.put("reason", "License purchase");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "PRO");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should return 400 when role is missing")
        void shouldReturn400WhenRoleIsMissing() {
            Map<String, Object> request = new HashMap<>();

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 400 when invalid role")
        void shouldReturn400WhenInvalidRole() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "INVALID");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "PRO");
            request.put("durationDays", 30);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("should prevent self-demotion from admin")
        void shouldPreventSelfDemotionFromAdmin() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "FREE");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("admin123")).thenReturn(Optional.of(adminUser));

            ResponseEntity<?> response = adminController.updateUserRole("admin123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should require duration for PRO role")
        void shouldRequireDurationForProRole() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "PRO");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            ResponseEntity<?> response = adminController.updateUserRole("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("extendTrial")
    class ExtendTrial {
        @Test
        @DisplayName("should extend trial when admin")
        void shouldExtendTrialWhenAdmin() {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 7);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            ResponseEntity<?> response = adminController.extendTrial("user123", request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 7);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.extendTrial("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 7);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            ResponseEntity<?> response = adminController.extendTrial("user123", request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("forceCheckExpiredRoles")
    class ForceCheckExpiredRoles {
        @Test
        @DisplayName("should check expired roles when admin")
        void shouldCheckExpiredRolesWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(roleExpiryService.forceCheckExpiredRoles()).thenReturn(3);

            ResponseEntity<?> response = adminController.forceCheckExpiredRoles();

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("revertedCount")).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.forceCheckExpiredRoles();

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("removeRoleExpiry")
    class RemoveRoleExpiry {
        @Test
        @DisplayName("should remove role expiry when admin")
        void shouldRemoveRoleExpiryWhenAdmin() {
            testUser.setRoleExpiryDate(LocalDateTime.now().plusDays(10));
            testUser.setOriginalRole(User.UserRole.FREE);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            ResponseEntity<?> response = adminController.removeRoleExpiry("user123");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.removeRoleExpiry("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            ResponseEntity<?> response = adminController.removeRoleExpiry("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {
        @Test
        @DisplayName("should return user when admin")
        void shouldReturnUserWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            ResponseEntity<?> response = adminController.getUserById("user123");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.getUserById("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            ResponseEntity<?> response = adminController.getUserById("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {
        @Test
        @DisplayName("should search users by email when admin")
        void shouldSearchUsersByEmailWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

            ResponseEntity<?> response = adminController.searchUsers("test");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body).containsKey("users");
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.searchUsers("test");

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {
        @Test
        @DisplayName("should delete user when admin")
        void shouldDeleteUserWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.existsById("user123")).thenReturn(true);
            doNothing().when(userRepository).deleteById("user123");

            ResponseEntity<?> response = adminController.deleteUser("user123");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userRepository).deleteById("user123");
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.deleteUser("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("should prevent self-deletion")
        void shouldPreventSelfDeletion() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);

            ResponseEntity<?> response = adminController.deleteUser("admin123");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            verify(userRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.existsById("user123")).thenReturn(false);

            ResponseEntity<?> response = adminController.deleteUser("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("getUserFeatureAccess")
    class GetUserFeatureAccess {
        @Test
        @DisplayName("should return feature access when admin")
        void shouldReturnFeatureAccessWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            UserFeatureAccess featureAccess = UserFeatureAccess.builder()
                    .id("access-1")
                    .userId("user123")
                    .incomePage(true)
                    .investmentPage(true)
                    .build();

            when(featureAccessService.getUserFeatureAccess(testUser)).thenReturn(featureAccess);

            ResponseEntity<?> response = adminController.getUserFeatureAccess("user123");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(featureAccess);
            verify(featureAccessService).getUserFeatureAccess(testUser);
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = adminController.getUserFeatureAccess("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(403);
            verify(featureAccessService, never()).getUserFeatureAccess(any());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            ResponseEntity<?> response = adminController.getUserFeatureAccess("user123");

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            verify(featureAccessService, never()).getUserFeatureAccess(any());
        }
    }

    @Nested
    @DisplayName("updateUserFeatureAccess")
    class UpdateUserFeatureAccess {
        @Test
        @DisplayName("should update feature access when admin")
        void shouldUpdateFeatureAccessWhenAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .incomePage(false)
                    .calendarPage(true)
                    .canExportPdf(true)
                    .build();

            UserFeatureAccess updatedAccess = UserFeatureAccess.builder()
                    .id("access-1")
                    .userId("user123")
                    .incomePage(false)
                    .calendarPage(true)
                    .canExportPdf(true)
                    .build();

            when(featureAccessService.updateFeatureAccess("user123", updates))
                    .thenReturn(updatedAccess);

            ResponseEntity<?> response = adminController.updateUserFeatureAccess("user123", updates);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body).containsKey("message");
            assertThat(body).containsKey("featureAccess");
            assertThat(body.get("message")).isEqualTo("Feature access updated successfully");
            verify(featureAccessService).updateFeatureAccess("user123", updates);
        }

        @Test
        @DisplayName("should set userId on feature access before updating")
        void shouldSetUserIdOnFeatureAccessBeforeUpdating() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .incomePage(false)
                    .build();

            UserFeatureAccess updatedAccess = UserFeatureAccess.builder()
                    .id("access-1")
                    .userId("user123")
                    .incomePage(false)
                    .build();

            when(featureAccessService.updateFeatureAccess("user123", updates))
                    .thenReturn(updatedAccess);

            adminController.updateUserFeatureAccess("user123", updates);

            assertThat(updates.getUserId()).isEqualTo("user123");
            verify(featureAccessService).updateFeatureAccess("user123", updates);
        }

        @Test
        @DisplayName("should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .incomePage(false)
                    .build();

            ResponseEntity<?> response = adminController.updateUserFeatureAccess("user123", updates);

            assertThat(response.getStatusCode().value()).isEqualTo(403);
            verify(featureAccessService, never()).updateFeatureAccess(anyString(), any());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.empty());

            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .incomePage(false)
                    .build();

            ResponseEntity<?> response = adminController.updateUserFeatureAccess("user123", updates);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            verify(featureAccessService, never()).updateFeatureAccess(anyString(), any());
        }

        @Test
        @DisplayName("should update investment types")
        void shouldUpdateInvestmentTypes() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            Set<String> newTypes = new HashSet<>(Arrays.asList("MUTUAL_FUND", "STOCK", "NPS"));
            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .allowedInvestmentTypes(newTypes)
                    .build();

            UserFeatureAccess updatedAccess = UserFeatureAccess.builder()
                    .id("access-1")
                    .userId("user123")
                    .allowedInvestmentTypes(newTypes)
                    .build();

            when(featureAccessService.updateFeatureAccess("user123", updates))
                    .thenReturn(updatedAccess);

            ResponseEntity<?> response = adminController.updateUserFeatureAccess("user123", updates);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(featureAccessService).updateFeatureAccess("user123", updates);
        }

        @Test
        @DisplayName("should update insurance restrictions")
        void shouldUpdateInsuranceRestrictions() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

            Set<String> newBlocked = new HashSet<>(Arrays.asList("VEHICLE", "PENSION"));
            UserFeatureAccess updates = UserFeatureAccess.builder()
                    .blockedInsuranceTypes(newBlocked)
                    .build();

            UserFeatureAccess updatedAccess = UserFeatureAccess.builder()
                    .id("access-1")
                    .userId("user123")
                    .blockedInsuranceTypes(newBlocked)
                    .build();

            when(featureAccessService.updateFeatureAccess("user123", updates))
                    .thenReturn(updatedAccess);

            ResponseEntity<?> response = adminController.updateUserFeatureAccess("user123", updates);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(featureAccessService).updateFeatureAccess("user123", updates);
        }
    }
}
