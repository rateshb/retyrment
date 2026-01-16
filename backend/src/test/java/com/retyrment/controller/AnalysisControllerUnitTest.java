package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.AnalysisService;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisController Unit Tests")
class AnalysisControllerUnitTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AnalysisController analysisController;

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

    @Nested
    @DisplayName("getNetWorth")
    class GetNetWorth {
        @Test
        @DisplayName("should return net worth data")
        void shouldReturnNetWorth() {
            Map<String, Object> mockNetWorth = new HashMap<>();
            mockNetWorth.put("totalAssets", 5000000.0);
            mockNetWorth.put("totalLiabilities", 1000000.0);
            mockNetWorth.put("netWorth", 4000000.0);
            when(analysisService.calculateNetWorth("user-1")).thenReturn(mockNetWorth);

            Map<String, Object> result = analysisController.getNetWorth();

            assertThat(result).containsKey("netWorth");
            assertThat(result.get("netWorth")).isEqualTo(4000000.0);
            verify(analysisService).calculateNetWorth("user-1");
        }
    }

    @Nested
    @DisplayName("getProjections")
    class GetProjections {
        @Test
        @DisplayName("should return projections for default years")
        void shouldReturnProjectionsDefaultYears() {
            Map<String, Object> mockProjections = new HashMap<>();
            mockProjections.put("years", 10);
            mockProjections.put("projectedValue", 8000000.0);
            when(analysisService.calculateProjections("user-1", 10)).thenReturn(mockProjections);

            Map<String, Object> result = analysisController.getProjections(10);

            assertThat(result).containsKey("projectedValue");
            verify(analysisService).calculateProjections("user-1", 10);
        }

        @Test
        @DisplayName("should return projections for custom years")
        void shouldReturnProjectionsCustomYears() {
            Map<String, Object> mockProjections = new HashMap<>();
            mockProjections.put("years", 20);
            when(analysisService.calculateProjections("user-1", 20)).thenReturn(mockProjections);

            Map<String, Object> result = analysisController.getProjections(20);

            assertThat(result).containsKey("years");
            verify(analysisService).calculateProjections("user-1", 20);
        }
    }

    @Nested
    @DisplayName("getGoalAnalysis")
    class GetGoalAnalysis {
        @Test
        @DisplayName("should return goal analysis")
        void shouldReturnGoalAnalysis() {
            Map<String, Object> mockAnalysis = new HashMap<>();
            mockAnalysis.put("goalsOnTrack", 3);
            mockAnalysis.put("goalsAtRisk", 1);
            when(analysisService.analyzeGoals("user-1")).thenReturn(mockAnalysis);

            Map<String, Object> result = analysisController.getGoalAnalysis();

            assertThat(result).containsKeys("goalsOnTrack", "goalsAtRisk");
            verify(analysisService).analyzeGoals("user-1");
        }
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendations {
        @Test
        @DisplayName("should return recommendations")
        void shouldReturnRecommendations() {
            Map<String, Object> mockRecommendations = new HashMap<>();
            mockRecommendations.put("recommendations", java.util.Arrays.asList("Increase SIP", "Reduce debt"));
            when(analysisService.generateRecommendations("user-1")).thenReturn(mockRecommendations);

            Map<String, Object> result = analysisController.getRecommendations();

            assertThat(result).containsKey("recommendations");
            verify(analysisService).generateRecommendations("user-1");
        }
    }

    @Nested
    @DisplayName("runMonteCarloSimulation")
    class RunMonteCarloSimulation {
        @Test
        @DisplayName("should run simulation with default params")
        void shouldRunSimulationDefault() {
            Map<String, Object> mockSimulation = new HashMap<>();
            mockSimulation.put("simulations", 1000);
            mockSimulation.put("successRate", 0.85);
            when(analysisService.runMonteCarloSimulation("user-1", 1000, 10)).thenReturn(mockSimulation);

            Map<String, Object> result = analysisController.runMonteCarloSimulation(1000, 10);

            assertThat(result).containsKey("successRate");
            verify(analysisService).runMonteCarloSimulation("user-1", 1000, 10);
        }

        @Test
        @DisplayName("should run simulation with custom params")
        void shouldRunSimulationCustom() {
            Map<String, Object> mockSimulation = new HashMap<>();
            mockSimulation.put("simulations", 5000);
            when(analysisService.runMonteCarloSimulation("user-1", 5000, 15)).thenReturn(mockSimulation);

            Map<String, Object> result = analysisController.runMonteCarloSimulation(5000, 15);

            assertThat(result).containsKey("simulations");
            verify(analysisService).runMonteCarloSimulation("user-1", 5000, 15);
        }
    }

    @Nested
    @DisplayName("getFullSummary")
    class GetFullSummary {
        @Test
        @DisplayName("should return full summary")
        void shouldReturnFullSummary() {
            Map<String, Object> mockSummary = new HashMap<>();
            mockSummary.put("netWorth", 4000000.0);
            mockSummary.put("projections", new HashMap<>());
            mockSummary.put("goals", new HashMap<>());
            when(analysisService.getFullSummary("user-1")).thenReturn(mockSummary);

            Map<String, Object> result = analysisController.getFullSummary();

            assertThat(result).containsKeys("netWorth", "projections", "goals");
            verify(analysisService).getFullSummary("user-1");
        }
    }
}
