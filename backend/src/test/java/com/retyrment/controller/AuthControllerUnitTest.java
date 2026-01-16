package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.model.UserFeatureAccess;
import com.retyrment.repository.UserRepository;
import com.retyrment.security.JwtUtils;
import com.retyrment.service.FeatureAccessService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FeatureAccessService featureAccessService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .name("Test User")
                .picture("https://example.com/pic.jpg")
                .role(User.UserRole.FREE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {
        @Test
        @DisplayName("should return user when authenticated")
        void shouldReturnUserWhenAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(testUser);

            ResponseEntity<?> response = authController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(com.retyrment.dto.UserResponseDTO.class);
            com.retyrment.dto.UserResponseDTO body = (com.retyrment.dto.UserResponseDTO) response.getBody();
            assertThat(body.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(null);

            ResponseEntity<?> response = authController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("should return 401 when anonymous user")
        void shouldReturn401WhenAnonymousUser() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            ResponseEntity<?> response = authController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("should return 401 when invalid principal")
        void shouldReturn401WhenInvalidPrincipal() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("invalid");

            ResponseEntity<?> response = authController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {
        @Test
        @DisplayName("should return valid response when token is valid")
        void shouldReturnValidWhenTokenIsValid() {
            Map<String, String> request = new HashMap<>();
            request.put("token", "valid-token");

            when(jwtUtils.validateToken("valid-token")).thenReturn(true);
            when(jwtUtils.getEmailFromToken("valid-token")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            ResponseEntity<?> response = authController.validateToken(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(true);
        }

        @Test
        @DisplayName("should return invalid when token is null")
        void shouldReturnInvalidWhenTokenIsNull() {
            Map<String, String> request = new HashMap<>();
            request.put("token", null);

            ResponseEntity<?> response = authController.validateToken(request);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(false);
        }

        @Test
        @DisplayName("should return invalid when token is invalid")
        void shouldReturnInvalidWhenTokenIsInvalid() {
            Map<String, String> request = new HashMap<>();
            request.put("token", "invalid-token");

            when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

            ResponseEntity<?> response = authController.validateToken(request);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(false);
        }

        @Test
        @DisplayName("should return invalid when user not found")
        void shouldReturnInvalidWhenUserNotFound() {
            Map<String, String> request = new HashMap<>();
            request.put("token", "valid-token");

            when(jwtUtils.validateToken("valid-token")).thenReturn(true);
            when(jwtUtils.getEmailFromToken("valid-token")).thenReturn("notfound@example.com");
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.validateToken(request);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {
        @Test
        @DisplayName("should clear security context and return success")
        void shouldClearSecurityContextAndReturnSuccess() {
            ResponseEntity<?> response = authController.logout();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("message")).isEqualTo("Logged out successfully");
        }
    }

    @Nested
    @DisplayName("getFeatureAccess")
    class GetFeatureAccess {
        @Test
        @DisplayName("should return features for FREE user")
        void shouldReturnFeaturesForFreeUser() {
            Map<String, Object> featureMap = new LinkedHashMap<>();
            featureMap.put("incomePage", true);
            featureMap.put("investmentPage", true);
            featureMap.put("retirementPage", true);
            featureMap.put("reportsPage", false);
            featureMap.put("simulationPage", false);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(featureAccessService.getFeatureAccessMap(testUser)).thenReturn(featureMap);

            ResponseEntity<?> response = authController.getFeatureAccess();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("role")).isEqualTo("FREE");
            assertThat(body.get("isPro")).isEqualTo(false);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) body.get("features");
            assertThat(features.get("dataEntry")).isEqualTo(true);
            assertThat(features.get("recommendations")).isEqualTo(false);
        }

        @Test
        @DisplayName("should return features for PRO user")
        void shouldReturnFeaturesForProUser() {
            User proUser = User.builder()
                    .id("pro123")
                    .email("pro@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(LocalDateTime.now())
                    .subscriptionEndDate(LocalDateTime.now().plusDays(30))
                    .build();

            Map<String, Object> featureMap = new LinkedHashMap<>();
            featureMap.put("incomePage", true);
            featureMap.put("reportsPage", true);
            featureMap.put("simulationPage", true);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(proUser);
            when(featureAccessService.getFeatureAccessMap(proUser)).thenReturn(featureMap);

            ResponseEntity<?> response = authController.getFeatureAccess();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("isPro")).isEqualTo(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) body.get("features");
            assertThat(features.get("recommendations")).isEqualTo(true);
            assertThat(features.get("monteCarlo")).isEqualTo(true);
        }

        @Test
        @DisplayName("should return features for ADMIN user")
        void shouldReturnFeaturesForAdminUser() {
            User adminUser = User.builder()
                    .id("admin123")
                    .email("admin@example.com")
                    .role(User.UserRole.ADMIN)
                    .build();

            Map<String, Object> featureMap = new LinkedHashMap<>();
            featureMap.put("adminPanel", true);
            featureMap.put("reportsPage", true);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(featureAccessService.getFeatureAccessMap(adminUser)).thenReturn(featureMap);

            ResponseEntity<?> response = authController.getFeatureAccess();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("isAdmin")).isEqualTo(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) body.get("features");
            assertThat(features.get("userManagement")).isEqualTo(true);
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(null);

            ResponseEntity<?> response = authController.getFeatureAccess();

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("should return user with trial information")
        void shouldReturnUserWithTrialInformation() {
            User trialUser = User.builder()
                    .id("trial123")
                    .email("trial@example.com")
                    .role(User.UserRole.FREE)
                    .trialStartDate(LocalDateTime.now().minusDays(2))
                    .trialEndDate(LocalDateTime.now().plusDays(5))
                    .build();

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(trialUser);

            ResponseEntity<?> response = authController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            com.retyrment.dto.UserResponseDTO body = (com.retyrment.dto.UserResponseDTO) response.getBody();
            assertThat(body.getTrial()).isNotNull();
            assertThat(body.getTrial().getActive()).isEqualTo(true);
        }
    }
}
