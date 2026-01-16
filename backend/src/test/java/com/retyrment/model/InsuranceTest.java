package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InsuranceTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build term life insurance")
        void shouldBuildTermLife() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("LIC Term Plan")
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .company("LIC")
                    .sumAssured(10000000.0)
                    .annualPremium(25000.0)
                    .premiumFrequency(Insurance.PremiumFrequency.YEARLY)
                    .startDate(LocalDate.of(2020, 4, 1))
                    .maturityDate(LocalDate.of(2050, 4, 1))
                    .renewalMonth(4)
                    .build();

            assertThat(insurance.getType()).isEqualTo(Insurance.InsuranceType.TERM_LIFE);
            assertThat(insurance.getSumAssured()).isEqualTo(10000000.0);
        }

        @Test
        @DisplayName("should build health insurance")
        void shouldBuildHealthInsurance() {
            Insurance insurance = Insurance.builder()
                    .policyName("HDFC Health Pro")
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(Insurance.HealthInsuranceType.FAMILY_FLOATER)
                    .company("HDFC Ergo")
                    .sumAssured(500000.0)
                    .annualPremium(15000.0)
                    .build();

            assertThat(insurance.getType()).isEqualTo(Insurance.InsuranceType.HEALTH);
            assertThat(insurance.getHealthType()).isEqualTo(Insurance.HealthInsuranceType.FAMILY_FLOATER);
        }

        @Test
        @DisplayName("should build ULIP with fund value")
        void shouldBuildUlip() {
            Insurance insurance = Insurance.builder()
                    .policyName("ICICI ULIP")
                    .type(Insurance.InsuranceType.ULIP)
                    .company("ICICI Prudential")
                    .sumAssured(2000000.0)
                    .annualPremium(50000.0)
                    .fundValue(250000.0)
                    .build();

            assertThat(insurance.getType()).isEqualTo(Insurance.InsuranceType.ULIP);
            assertThat(insurance.getFundValue()).isEqualTo(250000.0);
        }

        @Test
        @DisplayName("should build annuity policy")
        void shouldBuildAnnuityPolicy() {
            Insurance insurance = Insurance.builder()
                    .policyName("LIC Pension Plan")
                    .type(Insurance.InsuranceType.ANNUITY)
                    .company("LIC")
                    .isAnnuityPolicy(true)
                    .annuityStartYear(2040)
                    .monthlyAnnuityAmount(50000.0)
                    .annuityGrowthRate(3.0)
                    .build();

            assertThat(insurance.getType()).isEqualTo(Insurance.InsuranceType.ANNUITY);
            assertThat(insurance.getIsAnnuityPolicy()).isTrue();
        }

        @Test
        @DisplayName("should build money-back policy")
        void shouldBuildMoneyBackPolicy() {
            Insurance insurance = Insurance.builder()
                    .policyName("LIC Money Back")
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .company("LIC")
                    .moneyBackYears("5,10,15")
                    .moneyBackPercent(20.0)
                    .maturityBenefit(500000.0)
                    .build();

            assertThat(insurance.getType()).isEqualTo(Insurance.InsuranceType.MONEY_BACK);
            assertThat(insurance.getMoneyBackPercent()).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("Insurance Types")
    class InsuranceTypes {

        @Test
        @DisplayName("should have all expected types")
        void shouldHaveAllTypes() {
            assertThat(Insurance.InsuranceType.values()).containsExactlyInAnyOrder(
                    Insurance.InsuranceType.TERM_LIFE,
                    Insurance.InsuranceType.HEALTH,
                    Insurance.InsuranceType.ULIP,
                    Insurance.InsuranceType.ENDOWMENT,
                    Insurance.InsuranceType.MONEY_BACK,
                    Insurance.InsuranceType.ANNUITY,
                    Insurance.InsuranceType.VEHICLE,
                    Insurance.InsuranceType.OTHER
            );
        }
    }

    @Nested
    @DisplayName("Health Insurance Types")
    class HealthInsuranceTypes {

        @Test
        @DisplayName("should have all health types")
        void shouldHaveAllHealthTypes() {
            assertThat(Insurance.HealthInsuranceType.values()).containsExactlyInAnyOrder(
                    Insurance.HealthInsuranceType.GROUP,
                    Insurance.HealthInsuranceType.PERSONAL,
                    Insurance.HealthInsuranceType.FAMILY_FLOATER
            );
        }
    }

    @Nested
    @DisplayName("Premium Frequencies")
    class PremiumFrequencies {

        @Test
        @DisplayName("should have all frequencies")
        void shouldHaveAllFrequencies() {
            assertThat(Insurance.PremiumFrequency.values()).containsExactlyInAnyOrder(
                    Insurance.PremiumFrequency.MONTHLY,
                    Insurance.PremiumFrequency.QUARTERLY,
                    Insurance.PremiumFrequency.HALF_YEARLY,
                    Insurance.PremiumFrequency.YEARLY,
                    Insurance.PremiumFrequency.SINGLE
            );
        }
    }

    @Nested
    @DisplayName("Premium Calculations")
    class PremiumCalculations {

        @Test
        @DisplayName("should calculate monthly equivalent for yearly premium")
        void shouldCalculateMonthlyFromYearly() {
            Insurance insurance = Insurance.builder()
                    .annualPremium(24000.0)
                    .premiumFrequency(Insurance.PremiumFrequency.YEARLY)
                    .build();

            double monthlyEquivalent = insurance.getAnnualPremium() / 12;
            assertThat(monthlyEquivalent).isEqualTo(2000.0);
        }
    }

    @Nested
    @DisplayName("Policy Duration")
    class PolicyDuration {

        @Test
        @DisplayName("should calculate policy term from dates")
        void shouldCalculatePolicyTerm() {
            Insurance insurance = Insurance.builder()
                    .startDate(LocalDate.of(2020, 1, 1))
                    .maturityDate(LocalDate.of(2050, 1, 1))
                    .build();

            int totalYears = insurance.getMaturityDate().getYear() - insurance.getStartDate().getYear();
            assertThat(totalYears).isEqualTo(30);
        }

        @Test
        @DisplayName("should identify active policy")
        void shouldIdentifyActivePolicy() {
            Insurance activePolicy = Insurance.builder()
                    .startDate(LocalDate.of(2020, 1, 1))
                    .maturityDate(LocalDate.of(2050, 1, 1))
                    .build();

            LocalDate today = LocalDate.now();
            boolean isActive = !today.isBefore(activePolicy.getStartDate())
                    && !today.isAfter(activePolicy.getMaturityDate());
            assertThat(isActive).isTrue();
        }
    }
}
