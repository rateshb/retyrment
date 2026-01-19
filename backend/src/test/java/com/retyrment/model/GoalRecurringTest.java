package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Goal model with recurring goal functionality.
 */
@DisplayName("Goal - Recurring Goals")
class GoalRecurringTest {

    @Nested
    @DisplayName("One-Time Goals")
    class OneTimeGoals {

        @Test
        @DisplayName("should return single occurrence for one-time goal")
        void shouldReturnSingleOccurrenceForOneTimeGoal() {
            Goal goal = Goal.builder()
                    .name("Child Education")
                    .targetAmount(2500000.0)
                    .targetYear(2030)
                    .isRecurring(false)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(1);
            assertThat(occurrences.get(0).getYear()).isEqualTo(2030);
            assertThat(occurrences.get(0).getAmount()).isEqualTo(2500000.0);
            assertThat(occurrences.get(0).getDescription()).isEqualTo("Child Education");
        }

        @Test
        @DisplayName("should return single occurrence when isRecurring is null")
        void shouldReturnSingleOccurrenceWhenIsRecurringNull() {
            Goal goal = Goal.builder()
                    .name("New Car")
                    .targetAmount(1500000.0)
                    .targetYear(2028)
                    .isRecurring(null)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(1);
        }

        @Test
        @DisplayName("should calculate occurrence count as 1 for one-time goal")
        void shouldCalculateOccurrenceCountAsOneForOneTimeGoal() {
            Goal goal = Goal.builder()
                    .name("Home Purchase")
                    .targetAmount(5000000.0)
                    .targetYear(2030)
                    .isRecurring(false)
                    .build();

            int count = goal.getOccurrenceCount(2045);

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Yearly Recurring Goals")
    class YearlyRecurringGoals {

        @Test
        @DisplayName("should expand yearly recurring goal to all years")
        void shouldExpandYearlyRecurringGoalToAllYears() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .targetAmount(200000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2029)
                    .adjustForInflation(false)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(5); // 2025, 2026, 2027, 2028, 2029
            assertThat(occurrences.get(0).getYear()).isEqualTo(2025);
            assertThat(occurrences.get(4).getYear()).isEqualTo(2029);
            // All amounts should be same (no inflation)
            assertThat(occurrences).allMatch(o -> o.getAmount() == 200000.0);
        }

        @Test
        @DisplayName("should apply inflation to recurring goal amounts")
        void shouldApplyInflationToRecurringGoalAmounts() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .targetAmount(200000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2027)
                    .adjustForInflation(true)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(3);
            assertThat(occurrences.get(0).getAmount()).isEqualTo(200000.0); // Year 0
            assertThat(occurrences.get(1).getAmount()).isCloseTo(212000.0, org.assertj.core.api.Assertions.within(100.0)); // 6% increase
            assertThat(occurrences.get(2).getAmount()).isCloseTo(224720.0, org.assertj.core.api.Assertions.within(100.0)); // 6% compounded
        }

        @Test
        @DisplayName("should use custom inflation rate when provided")
        void shouldUseCustomInflationRateWhenProvided() {
            Goal goal = Goal.builder()
                    .name("School Fees")
                    .targetAmount(100000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2026)
                    .adjustForInflation(true)
                    .customInflationRate(10.0) // 10% education inflation
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(2);
            assertThat(occurrences.get(0).getAmount()).isEqualTo(100000.0);
            assertThat(occurrences.get(1).getAmount()).isCloseTo(110000.0, org.assertj.core.api.Assertions.within(100.0)); // 10% increase
        }

        @Test
        @DisplayName("should use retirement year as end year when recurrenceEndYear is null")
        void shouldUseRetirementYearAsEndYearWhenRecurrenceEndYearIsNull() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .targetAmount(200000.0)
                    .targetYear(2040)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(null)
                    .adjustForInflation(false)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(6); // 2040 to 2045
            assertThat(occurrences.get(occurrences.size() - 1).getYear()).isEqualTo(2045);
        }
    }

    @Nested
    @DisplayName("Multi-Year Interval Goals")
    class MultiYearIntervalGoals {

        @Test
        @DisplayName("should expand goal with 2-year interval")
        void shouldExpandGoalWith2YearInterval() {
            Goal goal = Goal.builder()
                    .name("Biennial Conference")
                    .targetAmount(150000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(2)
                    .recurrenceEndYear(2031)
                    .adjustForInflation(false)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(4); // 2025, 2027, 2029, 2031
            assertThat(occurrences.get(0).getYear()).isEqualTo(2025);
            assertThat(occurrences.get(1).getYear()).isEqualTo(2027);
            assertThat(occurrences.get(2).getYear()).isEqualTo(2029);
            assertThat(occurrences.get(3).getYear()).isEqualTo(2031);
        }

        @Test
        @DisplayName("should expand goal with 5-year interval for car replacement")
        void shouldExpandGoalWith5YearIntervalForCarReplacement() {
            Goal goal = Goal.builder()
                    .name("Car Replacement")
                    .targetAmount(1500000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(5)
                    .recurrenceEndYear(2045)
                    .adjustForInflation(true)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(5); // 2025, 2030, 2035, 2040, 2045
            assertThat(occurrences.get(0).getYear()).isEqualTo(2025);
            assertThat(occurrences.get(1).getYear()).isEqualTo(2030);
            assertThat(occurrences.get(2).getYear()).isEqualTo(2035);
            
            // Check inflation applied correctly (5 years compound)
            double expected2030 = 1500000.0 * Math.pow(1.06, 5);
            assertThat(occurrences.get(1).getAmount()).isCloseTo(expected2030, org.assertj.core.api.Assertions.within(1000.0));
        }

        @Test
        @DisplayName("should use default interval of 1 when recurrenceInterval is null")
        void shouldUseDefaultIntervalOfOneWhenRecurrenceIntervalIsNull() {
            Goal goal = Goal.builder()
                    .name("Annual Event")
                    .targetAmount(100000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(null)
                    .recurrenceEndYear(2027)
                    .adjustForInflation(false)
                    .build();

            List<Goal.GoalOccurrence> occurrences = goal.expandOccurrences(2045, 6.0);

            assertThat(occurrences).hasSize(3); // 2025, 2026, 2027
        }
    }

    @Nested
    @DisplayName("Total Cost Calculation")
    class TotalCostCalculation {

        @Test
        @DisplayName("should calculate total cost for one-time goal")
        void shouldCalculateTotalCostForOneTimeGoal() {
            Goal goal = Goal.builder()
                    .name("Home Purchase")
                    .targetAmount(5000000.0)
                    .targetYear(2030)
                    .isRecurring(false)
                    .build();

            double totalCost = goal.getTotalCost(2045, 6.0);

            assertThat(totalCost).isEqualTo(5000000.0);
        }

        @Test
        @DisplayName("should calculate total cost for recurring goal without inflation")
        void shouldCalculateTotalCostForRecurringGoalWithoutInflation() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .targetAmount(200000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2029)
                    .adjustForInflation(false)
                    .build();

            double totalCost = goal.getTotalCost(2045, 6.0);

            assertThat(totalCost).isEqualTo(1000000.0); // 5 × 200000
        }

        @Test
        @DisplayName("should calculate total cost for recurring goal with inflation")
        void shouldCalculateTotalCostForRecurringGoalWithInflation() {
            Goal goal = Goal.builder()
                    .name("Annual Event")
                    .targetAmount(100000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2027)
                    .adjustForInflation(true)
                    .build();

            double totalCost = goal.getTotalCost(2045, 6.0);

            // 100000 + 106000 + 112360 = 318360
            assertThat(totalCost).isCloseTo(318360.0, org.assertj.core.api.Assertions.within(100.0));
        }
    }

    @Nested
    @DisplayName("Occurrence Count")
    class OccurrenceCount {

        @Test
        @DisplayName("should calculate correct occurrence count for yearly recurring")
        void shouldCalculateCorrectOccurrenceCountForYearlyRecurring() {
            Goal goal = Goal.builder()
                    .name("Annual Vacation")
                    .targetAmount(200000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(1)
                    .recurrenceEndYear(2029)
                    .build();

            int count = goal.getOccurrenceCount(2045);

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("should calculate correct occurrence count for 5-year interval")
        void shouldCalculateCorrectOccurrenceCountFor5YearInterval() {
            Goal goal = Goal.builder()
                    .name("Car Replacement")
                    .targetAmount(1500000.0)
                    .targetYear(2025)
                    .isRecurring(true)
                    .recurrenceInterval(5)
                    .recurrenceEndYear(2045)
                    .build();

            int count = goal.getOccurrenceCount(2045);

            assertThat(count).isEqualTo(5); // 2025, 2030, 2035, 2040, 2045
        }
    }
}
