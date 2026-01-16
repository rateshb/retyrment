package com.retyrment.controller;

import com.retyrment.model.RetirementScenario;
import com.retyrment.model.User;
import com.retyrment.model.UserStrategy;
import com.retyrment.repository.RetirementScenarioRepository;
import com.retyrment.repository.UserStrategyRepository;
import com.retyrment.service.RetirementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetirementController Unit Tests")
class RetirementControllerUnitTest {

    @Mock
    private RetirementScenarioRepository scenarioRepository;

    @Mock
    private UserStrategyRepository userStrategyRepository;

    @Mock
    private RetirementService retirementService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RetirementController retirementController;

    private User testUser;
    private RetirementScenario testScenario;
    private UserStrategy testStrategy;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .role(User.UserRole.PRO)
                .build();
        
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);

        testScenario = RetirementScenario.builder()
                .id("scenario1")
                .name("Test Scenario")
                .currentAge(35)
                .retirementAge(60)
                .isDefault(false)
                .build();

        testStrategy = UserStrategy.builder()
                .id("strategy1")
                .userId("user123")
                .selectedIncomeStrategy("SUSTAINABLE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getRetirementMatrix")
    class GetRetirementMatrix {
        @Test
        @DisplayName("should return retirement matrix")
        void shouldReturnRetirementMatrix() {
            Map<String, Object> mockMatrix = new HashMap<>();
            mockMatrix.put("matrix", Collections.emptyList());
            mockMatrix.put("summary", new HashMap<>());

            when(retirementService.generateRetirementMatrix("user123", null)).thenReturn(mockMatrix);

            Map<String, Object> result = retirementController.getRetirementMatrix();

            assertThat(result).containsKey("matrix");
            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("calculateWithAssumptions")
    class CalculateWithAssumptions {
        @Test
        @DisplayName("should calculate with scenario assumptions")
        void shouldCalculateWithScenarioAssumptions() {
            Map<String, Object> mockMatrix = new HashMap<>();
            mockMatrix.put("matrix", Collections.emptyList());

            when(retirementService.generateRetirementMatrix(eq("user123"), any(RetirementScenario.class))).thenReturn(mockMatrix);

            Map<String, Object> result = retirementController.calculateWithAssumptions(testScenario);

            assertThat(result).containsKey("matrix");
            verify(retirementService).generateRetirementMatrix(eq("user123"), any(RetirementScenario.class));
        }
    }

    @Nested
    @DisplayName("getAllScenarios")
    class GetAllScenarios {
        @Test
        @DisplayName("should return all scenarios")
        void shouldReturnAllScenarios() {
            when(scenarioRepository.findAll()).thenReturn(Arrays.asList(testScenario));

            List<RetirementScenario> result = retirementController.getAllScenarios();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Scenario");
        }
    }

    @Nested
    @DisplayName("getDefaultScenario")
    class GetDefaultScenario {
        @Test
        @DisplayName("should return default scenario when exists")
        void shouldReturnDefaultScenarioWhenExists() {
            RetirementScenario defaultScenario = RetirementScenario.builder()
                    .id("default1")
                    .isDefault(true)
                    .build();

            when(scenarioRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultScenario));

            ResponseEntity<RetirementScenario> response = retirementController.getDefaultScenario();

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("should return 404 when no default scenario")
        void shouldReturn404WhenNoDefaultScenario() {
            when(scenarioRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

            ResponseEntity<RetirementScenario> response = retirementController.getDefaultScenario();

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("getScenarioById")
    class GetScenarioById {
        @Test
        @DisplayName("should return scenario when found")
        void shouldReturnScenarioWhenFound() {
            when(scenarioRepository.findById("scenario1")).thenReturn(Optional.of(testScenario));

            ResponseEntity<RetirementScenario> response = retirementController.getScenarioById("scenario1");

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() {
            when(scenarioRepository.findById("scenario1")).thenReturn(Optional.empty());

            ResponseEntity<RetirementScenario> response = retirementController.getScenarioById("scenario1");

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("createScenario")
    class CreateScenario {
        @Test
        @DisplayName("should create scenario")
        void shouldCreateScenario() {
            RetirementScenario newScenario = RetirementScenario.builder()
                    .name("New Scenario")
                    .isDefault(false)
                    .build();

            when(scenarioRepository.save(any(RetirementScenario.class))).thenReturn(newScenario);

            RetirementScenario result = retirementController.createScenario(newScenario);

            assertThat(result).isNotNull();
            verify(scenarioRepository).save(any(RetirementScenario.class));
            // When isDefault is false, findByIsDefaultTrue is not called
            verify(scenarioRepository, never()).findByIsDefaultTrue();
        }

        @Test
        @DisplayName("should unset other defaults when creating new default")
        void shouldUnsetOtherDefaultsWhenCreatingNewDefault() {
            RetirementScenario existingDefault = RetirementScenario.builder()
                    .id("old-default")
                    .isDefault(true)
                    .build();

            RetirementScenario newDefault = RetirementScenario.builder()
                    .name("New Default")
                    .isDefault(true)
                    .build();

            when(scenarioRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingDefault));
            when(scenarioRepository.save(any(RetirementScenario.class))).thenAnswer(invocation -> {
                RetirementScenario scenario = invocation.getArgument(0);
                if (scenario.getId() != null && scenario.getId().equals("old-default")) {
                    existingDefault.setIsDefault(false);
                    return existingDefault;
                }
                return newDefault;
            });

            retirementController.createScenario(newDefault);

            verify(scenarioRepository, times(2)).save(any(RetirementScenario.class));
            assertThat(existingDefault.getIsDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateScenario")
    class UpdateScenario {
        @Test
        @DisplayName("should update scenario when exists")
        void shouldUpdateScenarioWhenExists() {
            when(scenarioRepository.existsById("scenario1")).thenReturn(true);
            when(scenarioRepository.save(any(RetirementScenario.class))).thenReturn(testScenario);

            ResponseEntity<RetirementScenario> response = retirementController.updateScenario("scenario1", testScenario);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(scenarioRepository).save(any(RetirementScenario.class));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() {
            when(scenarioRepository.existsById("scenario1")).thenReturn(false);

            ResponseEntity<RetirementScenario> response = retirementController.updateScenario("scenario1", testScenario);

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
            verify(scenarioRepository, never()).save(any(RetirementScenario.class));
        }
    }

    @Nested
    @DisplayName("deleteScenario")
    class DeleteScenario {
        @Test
        @DisplayName("should delete scenario when exists")
        void shouldDeleteScenarioWhenExists() {
            when(scenarioRepository.existsById("scenario1")).thenReturn(true);
            doNothing().when(scenarioRepository).deleteById("scenario1");

            ResponseEntity<Void> response = retirementController.deleteScenario("scenario1");

            assertThat(response.getStatusCodeValue()).isEqualTo(204);
            verify(scenarioRepository).deleteById("scenario1");
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() {
            when(scenarioRepository.existsById("scenario1")).thenReturn(false);

            ResponseEntity<Void> response = retirementController.deleteScenario("scenario1");

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
            verify(scenarioRepository, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("getMaturingBeforeRetirement")
    class GetMaturingBeforeRetirement {
        @Test
        @DisplayName("should return maturing investments")
        void shouldReturnMaturingInvestments() {
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("totalMaturityValue", 100000L);

            when(retirementService.calculateMaturingBeforeRetirement("user123", 35, 60)).thenReturn(mockResult);

            Map<String, Object> result = retirementController.getMaturingBeforeRetirement(35, 60);

            assertThat(result).containsKey("totalMaturityValue");
            verify(retirementService).calculateMaturingBeforeRetirement("user123", 35, 60);
        }

        @Test
        @DisplayName("should use default parameters")
        void shouldUseDefaultParameters() {
            Map<String, Object> mockResult = new HashMap<>();
            when(retirementService.calculateMaturingBeforeRetirement("user123", 35, 60)).thenReturn(mockResult);

            retirementController.getMaturingBeforeRetirement(35, 60);

            verify(retirementService).calculateMaturingBeforeRetirement("user123", 35, 60);
        }
    }

    @Nested
    @DisplayName("getUserStrategy")
    class GetUserStrategy {
        @Test
        @DisplayName("should return user strategy when exists")
        void shouldReturnUserStrategyWhenExists() {
            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.of(testStrategy));

            ResponseEntity<UserStrategy> response = retirementController.getUserStrategy(testUser);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should return 204 when no strategy")
        void shouldReturn204WhenNoStrategy() {
            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.empty());

            ResponseEntity<UserStrategy> response = retirementController.getUserStrategy(testUser);

            assertThat(response.getStatusCodeValue()).isEqualTo(204);
        }

        @Test
        @DisplayName("should return 400 when user is null")
        void shouldReturn400WhenUserIsNull() {
            ResponseEntity<UserStrategy> response = retirementController.getUserStrategy(null);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("saveUserStrategy")
    class SaveUserStrategy {
        @Test
        @DisplayName("should create new strategy")
        void shouldCreateNewStrategy() {
            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.empty());
            when(userStrategyRepository.save(any(UserStrategy.class))).thenReturn(testStrategy);

            ResponseEntity<UserStrategy> response = retirementController.saveUserStrategy(testUser, testStrategy);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userStrategyRepository).save(any(UserStrategy.class));
        }

        @Test
        @DisplayName("should update existing strategy")
        void shouldUpdateExistingStrategy() {
            UserStrategy existing = UserStrategy.builder()
                    .id("strategy1")
                    .userId("user123")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.of(existing));
            when(userStrategyRepository.save(any(UserStrategy.class))).thenReturn(testStrategy);

            ResponseEntity<UserStrategy> response = retirementController.saveUserStrategy(testUser, testStrategy);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(userStrategyRepository).save(any(UserStrategy.class));
        }

        @Test
        @DisplayName("should return 400 when user is null")
        void shouldReturn400WhenUserIsNull() {
            ResponseEntity<UserStrategy> response = retirementController.saveUserStrategy(null, testStrategy);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
            verify(userStrategyRepository, never()).save(any(UserStrategy.class));
        }
    }

    @Nested
    @DisplayName("deleteUserStrategy")
    class DeleteUserStrategy {
        @Test
        @DisplayName("should delete user strategy when exists")
        void shouldDeleteUserStrategyWhenExists() {
            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.of(testStrategy));
            doNothing().when(userStrategyRepository).deleteById("strategy1");

            ResponseEntity<Void> response = retirementController.deleteUserStrategy(testUser);

            assertThat(response.getStatusCodeValue()).isEqualTo(204);
            verify(userStrategyRepository).deleteById("strategy1");
        }

        @Test
        @DisplayName("should return 204 when no strategy exists")
        void shouldReturn204WhenNoStrategyExists() {
            when(userStrategyRepository.findByUserId("user123")).thenReturn(Optional.empty());

            ResponseEntity<Void> response = retirementController.deleteUserStrategy(testUser);

            assertThat(response.getStatusCodeValue()).isEqualTo(204);
            verify(userStrategyRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("should return 400 when user is null")
        void shouldReturn400WhenUserIsNull() {
            ResponseEntity<Void> response = retirementController.deleteUserStrategy(null);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
        }
    }
}
