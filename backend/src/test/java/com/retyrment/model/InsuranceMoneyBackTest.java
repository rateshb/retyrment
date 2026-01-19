package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Insurance model with money-back payout functionality.
 */
@DisplayName("Insurance - Money-Back Payouts")
class InsuranceMoneyBackTest {

    @Nested
    @DisplayName("MoneyBackPayout")
    class MoneyBackPayoutTests {

        @Test
        @DisplayName("should calculate payout based on percentage")
        void shouldCalculatePayoutBasedOnPercentage() {
            Insurance.MoneyBackPayout payout = Insurance.MoneyBackPayout.builder()
                    .policyYear(5)
                    .percentage(20.0)
                    .includesBonus(false)
                    .build();

            double amount = payout.calculatePayout(1000000.0, 0);

            assertThat(amount).isEqualTo(200000.0); // 20% of 10L
        }

        @Test
        @DisplayName("should calculate payout based on fixed amount")
        void shouldCalculatePayoutBasedOnFixedAmount() {
            Insurance.MoneyBackPayout payout = Insurance.MoneyBackPayout.builder()
                    .policyYear(10)
                    .fixedAmount(150000.0)
                    .includesBonus(false)
                    .build();

            double amount = payout.calculatePayout(1000000.0, 0);

            assertThat(amount).isEqualTo(150000.0);
        }

        @Test
        @DisplayName("should include bonus when flag is set")
        void shouldIncludeBonusWhenFlagIsSet() {
            Insurance.MoneyBackPayout payout = Insurance.MoneyBackPayout.builder()
                    .policyYear(15)
                    .percentage(30.0)
                    .includesBonus(true)
                    .build();

            double amount = payout.calculatePayout(1000000.0, 200000.0);

            // 30% of sum assured + 30% of bonus = 300000 + 60000 = 360000
            assertThat(amount).isEqualTo(360000.0);
        }

        @Test
        @DisplayName("should prefer percentage over fixed amount")
        void shouldPreferPercentageOverFixedAmount() {
            Insurance.MoneyBackPayout payout = Insurance.MoneyBackPayout.builder()
                    .policyYear(5)
                    .percentage(20.0)
                    .fixedAmount(100000.0)
                    .includesBonus(false)
                    .build();

            double amount = payout.calculatePayout(1000000.0, 0);

            assertThat(amount).isEqualTo(200000.0); // Uses percentage
        }

        @Test
        @DisplayName("should return zero when no percentage or amount set")
        void shouldReturnZeroWhenNoPercentageOrAmountSet() {
            Insurance.MoneyBackPayout payout = Insurance.MoneyBackPayout.builder()
                    .policyYear(5)
                    .build();

            double amount = payout.calculatePayout(1000000.0, 0);

            assertThat(amount).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Insurance with Multiple Payouts")
    class InsuranceWithMultiplePayouts {

        @Test
        @DisplayName("should return all money-back payouts")
        void shouldReturnAllMoneyBackPayouts() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder().policyYear(5).percentage(20.0).build(),
                            Insurance.MoneyBackPayout.builder().policyYear(10).percentage(30.0).build(),
                            Insurance.MoneyBackPayout.builder().policyYear(15).percentage(50.0).build()
                    ))
                    .build();

            List<Insurance.MoneyBackPayout> payouts = policy.getAllMoneyBackPayouts();

            assertThat(payouts).hasSize(3);
            assertThat(payouts.get(0).getPolicyYear()).isEqualTo(5);
            assertThat(payouts.get(1).getPolicyYear()).isEqualTo(10);
            assertThat(payouts.get(2).getPolicyYear()).isEqualTo(15);
        }

        @Test
        @DisplayName("should calculate total money-back payouts")
        void shouldCalculateTotalMoneyBackPayouts() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .bonusAccrued(100000.0)
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder().policyYear(5).percentage(20.0).includesBonus(false).build(),
                            Insurance.MoneyBackPayout.builder().policyYear(10).percentage(30.0).includesBonus(false).build()
                    ))
                    .build();

            double total = policy.getTotalMoneyBackPayouts();

            assertThat(total).isEqualTo(500000.0); // 200000 + 300000
        }

        @Test
        @DisplayName("should generate payout schedule with calendar years")
        void shouldGeneratePayoutScheduleWithCalendarYears() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .startDate(LocalDate.of(2020, 1, 1))
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder().policyYear(5).percentage(20.0).build(),
                            Insurance.MoneyBackPayout.builder().policyYear(10).percentage(30.0).build()
                    ))
                    .build();

            List<Insurance.PayoutSchedule> schedule = policy.getPayoutSchedule();

            assertThat(schedule).hasSize(2);
            assertThat(schedule.get(0).getCalendarYear()).isEqualTo(2025); // 2020 + 5
            assertThat(schedule.get(0).getAmount()).isEqualTo(200000.0);
            assertThat(schedule.get(1).getCalendarYear()).isEqualTo(2030); // 2020 + 10
            assertThat(schedule.get(1).getAmount()).isEqualTo(300000.0);
        }

        @Test
        @DisplayName("should return empty schedule for non-money-back policy")
        void shouldReturnEmptyScheduleForNonMoneyBackPolicy() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .sumAssured(1000000.0)
                    .build();

            List<Insurance.PayoutSchedule> schedule = policy.getPayoutSchedule();

            assertThat(schedule).isEmpty();
        }
    }

    @Nested
    @DisplayName("Legacy Field Migration")
    class LegacyFieldMigration {

        @Test
        @DisplayName("should convert legacy moneyBackYears to payout list")
        void shouldConvertLegacyMoneyBackYearsToPayout() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .moneyBackYears("5,10,15")
                    .moneyBackPercent(20.0)
                    .build();

            List<Insurance.MoneyBackPayout> payouts = policy.getAllMoneyBackPayouts();

            assertThat(payouts).hasSize(3);
            assertThat(payouts.get(0).getPolicyYear()).isEqualTo(5);
            assertThat(payouts.get(0).getPercentage()).isEqualTo(20.0);
            assertThat(payouts.get(1).getPolicyYear()).isEqualTo(10);
            assertThat(payouts.get(2).getPolicyYear()).isEqualTo(15);
        }

        @Test
        @DisplayName("should prefer new payout list over legacy fields")
        void shouldPreferNewPayoutListOverLegacyFields() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .moneyBackYears("5,10")
                    .moneyBackPercent(20.0)
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder().policyYear(5).percentage(25.0).build()
                    ))
                    .build();

            List<Insurance.MoneyBackPayout> payouts = policy.getAllMoneyBackPayouts();

            assertThat(payouts).hasSize(1);
            assertThat(payouts.get(0).getPercentage()).isEqualTo(25.0); // Uses new list, not legacy
        }

        @Test
        @DisplayName("should handle invalid legacy year gracefully")
        void shouldHandleInvalidLegacyYearGracefully() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .moneyBackYears("5,invalid,15")
                    .moneyBackPercent(20.0)
                    .build();

            List<Insurance.MoneyBackPayout> payouts = policy.getAllMoneyBackPayouts();

            assertThat(payouts).hasSize(2); // Only 5 and 15 parsed
        }

        @Test
        @DisplayName("should return empty list when no payouts defined")
        void shouldReturnEmptyListWhenNoPayoutsDefined() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .build();

            List<Insurance.MoneyBackPayout> payouts = policy.getAllMoneyBackPayouts();

            assertThat(payouts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("should handle LIC Jeevan Shiromani style policy")
        void shouldHandleLicJeevanShiromaniStylePolicy() {
            // LIC Jeevan Shiromani: 15L sum assured, 20-30-50 payout pattern
            Insurance policy = Insurance.builder()
                    .policyName("LIC Jeevan Shiromani")
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1500000.0)
                    .startDate(LocalDate.of(2020, 4, 1))
                    .policyTerm(20)
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(5)
                                    .percentage(20.0)
                                    .includesBonus(false)
                                    .description("Survival Benefit 1")
                                    .build(),
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(10)
                                    .percentage(30.0)
                                    .includesBonus(false)
                                    .description("Survival Benefit 2")
                                    .build(),
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(15)
                                    .percentage(50.0)
                                    .includesBonus(true)
                                    .description("Final Survival Benefit + Bonus")
                                    .build()
                    ))
                    .bonusAccrued(300000.0)
                    .build();

            List<Insurance.PayoutSchedule> schedule = policy.getPayoutSchedule();

            assertThat(schedule).hasSize(3);
            
            // Year 5: 20% of 15L = 3L
            assertThat(schedule.get(0).getCalendarYear()).isEqualTo(2025);
            assertThat(schedule.get(0).getAmount()).isEqualTo(300000.0);
            
            // Year 10: 30% of 15L = 4.5L
            assertThat(schedule.get(1).getCalendarYear()).isEqualTo(2030);
            assertThat(schedule.get(1).getAmount()).isEqualTo(450000.0);
            
            // Year 15: 50% of 15L + 50% of 3L bonus = 7.5L + 1.5L = 9L
            assertThat(schedule.get(2).getCalendarYear()).isEqualTo(2035);
            assertThat(schedule.get(2).getAmount()).isEqualTo(900000.0);
            
            // Total payouts = 3L + 4.5L + 9L = 16.5L
            assertThat(policy.getTotalMoneyBackPayouts()).isEqualTo(1650000.0);
        }

        @Test
        @DisplayName("should handle HDFC Sanchay Plus style policy with fixed amounts")
        void shouldHandleHdfcSanchayPlusStylePolicy() {
            // Hypothetical policy with fixed amounts
            Insurance policy = Insurance.builder()
                    .policyName("HDFC Sanchay Plus")
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .sumAssured(1000000.0)
                    .startDate(LocalDate.of(2022, 6, 1))
                    .moneyBackPayouts(Arrays.asList(
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(5)
                                    .fixedAmount(100000.0)
                                    .build(),
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(10)
                                    .fixedAmount(150000.0)
                                    .build(),
                            Insurance.MoneyBackPayout.builder()
                                    .policyYear(15)
                                    .fixedAmount(200000.0)
                                    .build()
                    ))
                    .build();

            double total = policy.getTotalMoneyBackPayouts();

            assertThat(total).isEqualTo(450000.0);
        }
    }
}
