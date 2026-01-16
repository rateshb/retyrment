package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Goal;
import com.retyrment.model.Goal.Priority;
import com.retyrment.model.User;
import com.retyrment.repository.GoalRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalController Unit Tests - Data Isolation")
class GoalControllerUnitTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GoalController goalController;

    private Goal testGoal;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testGoal = Goal.builder()
                .id("goal-1")
                .userId("user-1")
                .name("Child Education")
                .targetAmount(2000000.0)
                .targetYear(2030)
                .priority(Priority.HIGH)
                .isRecurring(false)
                .build();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllGoals - Data Isolation")
    class GetAllGoals {
        @Test
        @DisplayName("should return only current user's goals")
        void shouldReturnOnlyCurrentUserGoals() {
            Goal user1Goal2 = Goal.builder().id("goal-2").userId("user-1").name("House").build();
            Goal user2Goal = Goal.builder().id("goal-3").userId("user-2").name("Other").build();
            
            when(goalRepository.findByUserIdOrderByTargetYearAsc("user-1"))
                    .thenReturn(Arrays.asList(testGoal, user1Goal2));

            List<Goal> result = goalController.getAllGoals();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(goal -> goal.getUserId().equals("user-1"));
            verify(goalRepository).findByUserIdOrderByTargetYearAsc("user-1");
        }
    }

    @Nested
    @DisplayName("getRecurringGoals - Data Isolation")
    class GetRecurringGoals {
        @Test
        @DisplayName("should return only current user's recurring goals")
        void shouldReturnOnlyCurrentUserRecurringGoals() {
            Goal recurringGoal = Goal.builder().id("goal-2").userId("user-1").isRecurring(true).build();
            when(goalRepository.findByUserIdAndIsRecurringTrue("user-1"))
                    .thenReturn(Arrays.asList(recurringGoal));

            List<Goal> result = goalController.getRecurringGoals();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
            assertThat(result.get(0).getIsRecurring()).isTrue();
        }
    }

    @Nested
    @DisplayName("getGoalById - Data Isolation")
    class GetGoalById {
        @Test
        @DisplayName("should return goal when it belongs to current user")
        void shouldReturnGoalWhenBelongsToUser() {
            when(goalRepository.findByIdAndUserId("goal-1", "user-1"))
                    .thenReturn(Optional.of(testGoal));

            Goal result = goalController.getGoalById("goal-1");

            assertThat(result.getId()).isEqualTo("goal-1");
            assertThat(result.getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should throw exception when goal belongs to another user")
        void shouldThrowExceptionWhenGoalBelongsToOtherUser() {
            when(goalRepository.findByIdAndUserId("goal-1", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalController.getGoalById("goal-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getGoalsByPriority - Branch Coverage")
    class GetGoalsByPriority {
        @Test
        @DisplayName("should filter goals by priority")
        void shouldFilterGoalsByPriority() {
            Goal highPriorityGoal = Goal.builder()
                    .id("goal-2")
                    .userId("user-1")
                    .priority(Priority.HIGH)
                    .build();
            Goal lowPriorityGoal = Goal.builder()
                    .id("goal-3")
                    .userId("user-1")
                    .priority(Priority.LOW)
                    .build();

            when(goalRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(testGoal, highPriorityGoal, lowPriorityGoal));

            List<Goal> result = goalController.getGoalsByPriority(Priority.HIGH);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(goal -> goal.getPriority() == Priority.HIGH);
        }

        @Test
        @DisplayName("should return empty list when no goals match priority")
        void shouldReturnEmptyListWhenNoMatch() {
            when(goalRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(testGoal));

            List<Goal> result = goalController.getGoalsByPriority(Priority.LOW);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUpcomingGoals - Branch Coverage")
    class GetUpcomingGoals {
        @Test
        @DisplayName("should filter goals by target year")
        void shouldFilterGoalsByTargetYear() {
            Goal goal2025 = Goal.builder()
                    .id("goal-2")
                    .userId("user-1")
                    .targetYear(2025)
                    .build();
            Goal goal2026 = Goal.builder()
                    .id("goal-3")
                    .userId("user-1")
                    .targetYear(2026)
                    .build();

            when(goalRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(goal2025, goal2026, testGoal));

            List<Goal> result = goalController.getUpcomingGoals(2026);

            // testGoal has targetYear 2030, so it should be excluded
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(goal -> goal.getTargetYear() != null && goal.getTargetYear() <= 2026);
        }

        @Test
        @DisplayName("should exclude goals with null targetYear")
        void shouldExcludeGoalsWithNullTargetYear() {
            Goal goalWithNullYear = Goal.builder()
                    .id("goal-2")
                    .userId("user-1")
                    .targetYear(null)
                    .build();
            Goal goal2025 = Goal.builder()
                    .id("goal-3")
                    .userId("user-1")
                    .targetYear(2025)
                    .build();

            when(goalRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(goalWithNullYear, goal2025));

            List<Goal> result = goalController.getUpcomingGoals(2026);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTargetYear()).isEqualTo(2025);
        }
    }

    @Nested
    @DisplayName("createGoal - Branch Coverage")
    class CreateGoal {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Goal newGoal = Goal.builder()
                    .name("New Goal")
                    .targetAmount(1000000.0)
                    .targetYear(2025)
                    .build();
            
            when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
                Goal g = inv.getArgument(0);
                g.setId("new-id");
                return g;
            });

            Goal result = goalController.createGoal(newGoal);

            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(goalRepository).save(argThat(goal -> goal.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should set default isRecurring to false when null")
        void shouldSetDefaultIsRecurringWhenNull() {
            Goal newGoal = Goal.builder()
                    .name("New Goal")
                    .targetAmount(1000000.0)
                    .targetYear(2025)
                    .isRecurring(null)
                    .build();
            
            when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

            goalController.createGoal(newGoal);

            verify(goalRepository).save(argThat(goal -> goal.getIsRecurring().equals(false)));
        }

        @Test
        @DisplayName("should set default priority to MEDIUM when null")
        void shouldSetDefaultPriorityWhenNull() {
            Goal newGoal = Goal.builder()
                    .name("New Goal")
                    .targetAmount(1000000.0)
                    .targetYear(2025)
                    .priority(null)
                    .build();
            
            when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

            goalController.createGoal(newGoal);

            verify(goalRepository).save(argThat(goal -> goal.getPriority().equals(Priority.MEDIUM)));
        }

        @Test
        @DisplayName("should preserve isRecurring when provided")
        void shouldPreserveIsRecurringWhenProvided() {
            Goal newGoal = Goal.builder()
                    .name("New Goal")
                    .targetAmount(1000000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .build();
            
            when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

            goalController.createGoal(newGoal);

            verify(goalRepository).save(argThat(goal -> goal.getIsRecurring().equals(true)));
        }

        @Test
        @DisplayName("should preserve priority when provided")
        void shouldPreservePriorityWhenProvided() {
            Goal newGoal = Goal.builder()
                    .name("New Goal")
                    .targetAmount(1000000.0)
                    .targetYear(2025)
                    .priority(Priority.HIGH)
                    .build();
            
            when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

            goalController.createGoal(newGoal);

            verify(goalRepository).save(argThat(goal -> goal.getPriority().equals(Priority.HIGH)));
        }
    }

    @Nested
    @DisplayName("updateGoal - Data Isolation")
    class UpdateGoal {
        @Test
        @DisplayName("should update goal when it belongs to current user")
        void shouldUpdateGoalWhenBelongsToUser() {
            when(goalRepository.findByIdAndUserId("goal-1", "user-1"))
                    .thenReturn(Optional.of(testGoal));
            when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

            Goal updated = Goal.builder().name("Updated Goal").build();
            Goal result = goalController.updateGoal("goal-1", updated);

            assertThat(result).isNotNull();
            verify(goalRepository).save(argThat(goal -> 
                goal.getId().equals("goal-1") && goal.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should throw exception when updating other user's goal")
        void shouldThrowExceptionWhenUpdatingOtherUserGoal() {
            when(goalRepository.findByIdAndUserId("goal-1", "user-1"))
                    .thenReturn(Optional.empty());

            Goal updated = Goal.builder().name("Updated").build();
            
            assertThatThrownBy(() -> goalController.updateGoal("goal-1", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGoal - Data Isolation")
    class DeleteGoal {
        @Test
        @DisplayName("should delete goal when it belongs to current user")
        void shouldDeleteGoalWhenBelongsToUser() {
            when(goalRepository.existsByIdAndUserId("goal-1", "user-1")).thenReturn(true);
            doNothing().when(goalRepository).deleteById("goal-1");

            goalController.deleteGoal("goal-1");

            verify(goalRepository).existsByIdAndUserId("goal-1", "user-1");
            verify(goalRepository).deleteById("goal-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's goal")
        void shouldThrowExceptionWhenDeletingOtherUserGoal() {
            when(goalRepository.existsByIdAndUserId("goal-1", "user-1")).thenReturn(false);

            assertThatThrownBy(() -> goalController.deleteGoal("goal-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
