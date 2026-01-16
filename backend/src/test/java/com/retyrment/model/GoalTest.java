package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GoalTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build retirement goal")
        void shouldBuildRetirementGoal() {
            Goal goal = Goal.builder()
                    .id("goal1")
                    .name("Retirement Corpus")
                    .icon("🎯")
                    .targetAmount(50000000.0)
                    .targetYear(2050)
                    .priority(Goal.Priority.HIGH)
                    .isRecurring(false)
                    .build();

            assertThat(goal.getName()).isEqualTo("Retirement Corpus");
            assertThat(goal.getTargetAmount()).isEqualTo(50000000.0);
            assertThat(goal.getPriority()).isEqualTo(Goal.Priority.HIGH);
        }

        @Test
        @DisplayName("should build education goal")
        void shouldBuildEducationGoal() {
            Goal goal = Goal.builder()
                    .name("Child Education")
                    .icon("📚")
                    .targetAmount(2500000.0)
                    .targetYear(2030)
                    .priority(Goal.Priority.HIGH)
                    .build();

            assertThat(goal.getName()).isEqualTo("Child Education");
            assertThat(goal.getTargetYear()).isEqualTo(2030);
        }

        @Test
        @DisplayName("should build recurring goal")
        void shouldBuildRecurringGoal() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .icon("✈️")
                    .targetAmount(200000.0)
                    .targetYear(2026)
                    .isRecurring(true)
                    .recurrence(Goal.RecurrenceType.YEARLY)
                    .build();

            assertThat(goal.getIsRecurring()).isTrue();
            assertThat(goal.getRecurrence()).isEqualTo(Goal.RecurrenceType.YEARLY);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameBlank() {
            Goal goal = Goal.builder()
                    .name("")
                    .targetAmount(1000000.0)
                    .targetYear(2030)
                    .build();

            Set<ConstraintViolation<Goal>> violations = validator.validate(goal);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when target amount is not positive")
        void shouldFailWhenAmountNotPositive() {
            Goal goal = Goal.builder()
                    .name("Test Goal")
                    .targetAmount(0.0)
                    .targetYear(2030)
                    .build();

            Set<ConstraintViolation<Goal>> violations = validator.validate(goal);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should pass for valid goal")
        void shouldPassForValidGoal() {
            Goal goal = Goal.builder()
                    .name("Valid Goal")
                    .targetAmount(500000.0)
                    .targetYear(2030)
                    .build();

            Set<ConstraintViolation<Goal>> violations = validator.validate(goal);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("should have all priorities")
        void shouldHaveAllPriorities() {
            assertThat(Goal.Priority.values()).containsExactlyInAnyOrder(
                    Goal.Priority.HIGH,
                    Goal.Priority.MEDIUM,
                    Goal.Priority.LOW
            );
        }
    }

    @Nested
    @DisplayName("Recurrence Types")
    class RecurrenceTypes {

        @Test
        @DisplayName("should have all recurrence types")
        void shouldHaveAllRecurrenceTypes() {
            assertThat(Goal.RecurrenceType.values()).containsExactlyInAnyOrder(
                    Goal.RecurrenceType.YEARLY,
                    Goal.RecurrenceType.NONE
            );
        }
    }

    @Nested
    @DisplayName("Goal Calculations")
    class GoalCalculations {

        @Test
        @DisplayName("should calculate years remaining")
        void shouldCalculateYearsRemaining() {
            Goal goal = Goal.builder()
                    .targetYear(2035)
                    .build();

            int currentYear = LocalDate.now().getYear();
            int yearsRemaining = goal.getTargetYear() - currentYear;
            assertThat(yearsRemaining).isPositive();
        }
    }
}
