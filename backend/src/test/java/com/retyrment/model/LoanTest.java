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

class LoanTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build valid home loan")
        void shouldBuildHomeLoan() {
            Loan homeLoan = Loan.builder()
                    .id("loan1")
                    .name("SBI Home Loan")
                    .type(Loan.LoanType.HOME)
                    .originalAmount(5000000.0)
                    .outstandingAmount(4500000.0)
                    .interestRate(8.5)
                    .emi(50000.0)
                    .tenureMonths(240)
                    .remainingMonths(216)
                    .startDate(LocalDate.of(2022, 6, 1))
                    .endDate(LocalDate.of(2042, 6, 1))
                    .build();

            assertThat(homeLoan.getType()).isEqualTo(Loan.LoanType.HOME);
            assertThat(homeLoan.getOriginalAmount()).isEqualTo(5000000.0);
            assertThat(homeLoan.getEmi()).isEqualTo(50000.0);
        }

        @Test
        @DisplayName("should build vehicle loan")
        void shouldBuildVehicleLoan() {
            Loan vehicleLoan = Loan.builder()
                    .name("HDFC Vehicle Loan")
                    .type(Loan.LoanType.VEHICLE)
                    .originalAmount(800000.0)
                    .outstandingAmount(600000.0)
                    .interestRate(9.0)
                    .emi(15000.0)
                    .tenureMonths(60)
                    .remainingMonths(48)
                    .startDate(LocalDate.now())
                    .build();

            assertThat(vehicleLoan.getType()).isEqualTo(Loan.LoanType.VEHICLE);
            assertThat(vehicleLoan.getTenureMonths()).isEqualTo(60);
        }

        @Test
        @DisplayName("should build personal loan")
        void shouldBuildPersonalLoan() {
            Loan personalLoan = Loan.builder()
                    .name("Personal Loan")
                    .type(Loan.LoanType.PERSONAL)
                    .originalAmount(200000.0)
                    .outstandingAmount(150000.0)
                    .interestRate(12.0)
                    .emi(8000.0)
                    .tenureMonths(36)
                    .remainingMonths(24)
                    .startDate(LocalDate.now())
                    .build();

            assertThat(personalLoan.getType()).isEqualTo(Loan.LoanType.PERSONAL);
        }

        @Test
        @DisplayName("should build education loan with moratorium")
        void shouldBuildEducationLoan() {
            Loan educationLoan = Loan.builder()
                    .name("Education Loan")
                    .type(Loan.LoanType.EDUCATION)
                    .originalAmount(1500000.0)
                    .outstandingAmount(1500000.0)
                    .interestRate(8.0)
                    .emi(15000.0)
                    .tenureMonths(120)
                    .remainingMonths(120)
                    .moratoriumMonths(12)
                    .startDate(LocalDate.now())
                    .build();

            assertThat(educationLoan.getType()).isEqualTo(Loan.LoanType.EDUCATION);
            assertThat(educationLoan.getMoratoriumMonths()).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameBlank() {
            Loan loan = Loan.builder()
                    .name("")
                    .type(Loan.LoanType.HOME)
                    .originalAmount(1000000.0)
                    .outstandingAmount(1000000.0)
                    .emi(10000.0)
                    .interestRate(8.5)
                    .tenureMonths(120)
                    .startDate(LocalDate.now())
                    .build();

            Set<ConstraintViolation<Loan>> violations = validator.validate(loan);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when type is null")
        void shouldFailWhenTypeNull() {
            Loan loan = Loan.builder()
                    .name("Test Loan")
                    .type(null)
                    .originalAmount(1000000.0)
                    .outstandingAmount(1000000.0)
                    .emi(10000.0)
                    .interestRate(8.5)
                    .tenureMonths(120)
                    .startDate(LocalDate.now())
                    .build();

            Set<ConstraintViolation<Loan>> violations = validator.validate(loan);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when original amount is not positive")
        void shouldFailWhenAmountNotPositive() {
            Loan loan = Loan.builder()
                    .name("Test Loan")
                    .type(Loan.LoanType.HOME)
                    .originalAmount(0.0)
                    .outstandingAmount(0.0)
                    .emi(10000.0)
                    .interestRate(8.5)
                    .tenureMonths(120)
                    .startDate(LocalDate.now())
                    .build();

            Set<ConstraintViolation<Loan>> violations = validator.validate(loan);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should pass for valid loan")
        void shouldPassForValidLoan() {
            Loan loan = Loan.builder()
                    .name("Valid Loan")
                    .type(Loan.LoanType.HOME)
                    .originalAmount(1000000.0)
                    .outstandingAmount(900000.0)
                    .interestRate(8.5)
                    .emi(10000.0)
                    .tenureMonths(120)
                    .remainingMonths(108)
                    .startDate(LocalDate.now())
                    .build();

            Set<ConstraintViolation<Loan>> violations = validator.validate(loan);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Loan Types")
    class LoanTypes {

        @Test
        @DisplayName("should have all expected types")
        void shouldHaveAllTypes() {
            assertThat(Loan.LoanType.values()).containsExactlyInAnyOrder(
                    Loan.LoanType.HOME,
                    Loan.LoanType.VEHICLE,
                    Loan.LoanType.PERSONAL,
                    Loan.LoanType.EDUCATION,
                    Loan.LoanType.CREDIT_CARD,
                    Loan.LoanType.OTHER
            );
        }
    }

    @Nested
    @DisplayName("Loan Calculations")
    class LoanCalculations {

        @Test
        @DisplayName("should calculate paid amount correctly")
        void shouldCalculatePaidAmount() {
            Loan loan = Loan.builder()
                    .originalAmount(1000000.0)
                    .outstandingAmount(800000.0)
                    .build();

            double paid = loan.getOriginalAmount() - loan.getOutstandingAmount();
            assertThat(paid).isEqualTo(200000.0);
        }

        @Test
        @DisplayName("should calculate payoff percentage")
        void shouldCalculatePayoffPercentage() {
            Loan loan = Loan.builder()
                    .originalAmount(1000000.0)
                    .outstandingAmount(600000.0)
                    .build();

            double payoffPercent = ((loan.getOriginalAmount() - loan.getOutstandingAmount()) / loan.getOriginalAmount()) * 100;
            assertThat(payoffPercent).isEqualTo(40.0);
        }
    }
}
