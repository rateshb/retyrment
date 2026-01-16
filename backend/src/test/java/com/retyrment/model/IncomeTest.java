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

class IncomeTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build salary income")
        void shouldBuildSalaryIncome() {
            Income salary = Income.builder()
                    .id("income1")
                    .source("Salary - TCS")
                    .monthlyAmount(150000.0)
                    .annualIncrement(8.0)
                    .startDate(LocalDate.of(2020, 1, 1))
                    .isActive(true)
                    .build();

            assertThat(salary.getSource()).isEqualTo("Salary - TCS");
            assertThat(salary.getMonthlyAmount()).isEqualTo(150000.0);
            assertThat(salary.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should build rental income")
        void shouldBuildRentalIncome() {
            Income rental = Income.builder()
                    .source("Rental - Whitefield Property")
                    .monthlyAmount(25000.0)
                    .annualIncrement(5.0)
                    .isActive(true)
                    .build();

            assertThat(rental.getSource()).contains("Rental");
            assertThat(rental.getMonthlyAmount()).isEqualTo(25000.0);
        }

        @Test
        @DisplayName("should build freelance income")
        void shouldBuildFreelanceIncome() {
            Income freelance = Income.builder()
                    .source("Freelance Consulting")
                    .monthlyAmount(50000.0)
                    .isActive(true)
                    .build();

            assertThat(freelance.getSource()).contains("Freelance");
        }

        @Test
        @DisplayName("should build inactive income")
        void shouldBuildInactiveIncome() {
            Income inactive = Income.builder()
                    .source("Previous Job")
                    .monthlyAmount(100000.0)
                    .isActive(false)
                    .build();

            assertThat(inactive.getIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should fail when source is blank")
        void shouldFailWhenSourceBlank() {
            Income income = Income.builder()
                    .source("")
                    .monthlyAmount(100000.0)
                    .build();

            Set<ConstraintViolation<Income>> violations = validator.validate(income);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when monthly amount is not positive")
        void shouldFailWhenAmountNotPositive() {
            Income income = Income.builder()
                    .source("Test Source")
                    .monthlyAmount(0.0)
                    .build();

            Set<ConstraintViolation<Income>> violations = validator.validate(income);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should pass for valid income")
        void shouldPassForValidIncome() {
            Income income = Income.builder()
                    .source("Valid Source")
                    .monthlyAmount(50000.0)
                    .annualIncrement(7.0)
                    .isActive(true)
                    .build();

            Set<ConstraintViolation<Income>> violations = validator.validate(income);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Income Calculations")
    class IncomeCalculations {

        @Test
        @DisplayName("should calculate annual from monthly")
        void shouldCalculateAnnualFromMonthly() {
            Income income = Income.builder()
                    .monthlyAmount(100000.0)
                    .build();

            double annual = income.getMonthlyAmount() * 12;
            assertThat(annual).isEqualTo(1200000.0);
        }

        @Test
        @DisplayName("should calculate next year income with increment")
        void shouldCalculateWithIncrement() {
            Income income = Income.builder()
                    .monthlyAmount(100000.0)
                    .annualIncrement(10.0)
                    .build();

            double nextYearMonthly = income.getMonthlyAmount() * (1 + income.getAnnualIncrement() / 100);
            assertThat(nextYearMonthly).isCloseTo(110000.0, org.assertj.core.data.Offset.offset(0.01));
        }
    }
}
