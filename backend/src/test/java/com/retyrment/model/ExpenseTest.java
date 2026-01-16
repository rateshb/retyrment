package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build rent expense")
        void shouldBuildRentExpense() {
            Expense expense = Expense.builder()
                    .id("exp1")
                    .name("House Rent")
                    .category(Expense.ExpenseCategory.RENT)
                    .monthlyAmount(35000.0)
                    .isFixed(true)
                    .build();

            assertThat(expense.getCategory()).isEqualTo(Expense.ExpenseCategory.RENT);
            assertThat(expense.getMonthlyAmount()).isEqualTo(35000.0);
            assertThat(expense.getIsFixed()).isTrue();
        }

        @Test
        @DisplayName("should build groceries expense")
        void shouldBuildGroceriesExpense() {
            Expense expense = Expense.builder()
                    .name("Monthly Groceries")
                    .category(Expense.ExpenseCategory.GROCERIES)
                    .monthlyAmount(15000.0)
                    .isFixed(false)
                    .build();

            assertThat(expense.getCategory()).isEqualTo(Expense.ExpenseCategory.GROCERIES);
        }

        @Test
        @DisplayName("should build subscription expense")
        void shouldBuildSubscriptionExpense() {
            Expense expense = Expense.builder()
                    .name("OTT Subscriptions")
                    .category(Expense.ExpenseCategory.SUBSCRIPTIONS)
                    .monthlyAmount(2000.0)
                    .isFixed(true)
                    .build();

            assertThat(expense.getCategory()).isEqualTo(Expense.ExpenseCategory.SUBSCRIPTIONS);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameBlank() {
            Expense expense = Expense.builder()
                    .name("")
                    .category(Expense.ExpenseCategory.RENT)
                    .monthlyAmount(10000.0)
                    .build();

            Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when category is null")
        void shouldFailWhenCategoryNull() {
            Expense expense = Expense.builder()
                    .name("Test Expense")
                    .category(null)
                    .monthlyAmount(10000.0)
                    .build();

            Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when monthly amount is not positive")
        void shouldFailWhenAmountNotPositive() {
            Expense expense = Expense.builder()
                    .name("Test Expense")
                    .category(Expense.ExpenseCategory.OTHER)
                    .monthlyAmount(-100.0)
                    .build();

            Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should pass for valid expense")
        void shouldPassForValidExpense() {
            Expense expense = Expense.builder()
                    .name("Valid Expense")
                    .category(Expense.ExpenseCategory.UTILITIES)
                    .amount(5000.0)  // Use new 'amount' field
                    .build();

            Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Expense Categories")
    class ExpenseCategories {

        @Test
        @DisplayName("should have all expected categories")
        void shouldHaveAllCategories() {
            assertThat(Expense.ExpenseCategory.values()).containsExactlyInAnyOrder(
                    Expense.ExpenseCategory.RENT,
                    Expense.ExpenseCategory.UTILITIES,
                    Expense.ExpenseCategory.GROCERIES,
                    Expense.ExpenseCategory.TRANSPORT,
                    Expense.ExpenseCategory.ENTERTAINMENT,
                    Expense.ExpenseCategory.HEALTHCARE,
                    Expense.ExpenseCategory.SHOPPING,
                    Expense.ExpenseCategory.DINING,
                    Expense.ExpenseCategory.TRAVEL,
                    Expense.ExpenseCategory.SUBSCRIPTIONS,
                    // Education-related (time-bound)
                    Expense.ExpenseCategory.SCHOOL_FEE,
                    Expense.ExpenseCategory.COLLEGE_FEE,
                    Expense.ExpenseCategory.TUITION,
                    Expense.ExpenseCategory.COACHING,
                    Expense.ExpenseCategory.BOOKS_SUPPLIES,
                    Expense.ExpenseCategory.HOSTEL,
                    // Dependent care (time-bound)
                    Expense.ExpenseCategory.CHILDCARE,
                    Expense.ExpenseCategory.DAYCARE,
                    Expense.ExpenseCategory.ELDERLY_CARE,
                    // Other
                    Expense.ExpenseCategory.MAINTENANCE,
                    Expense.ExpenseCategory.SOCIETY_CHARGES,
                    Expense.ExpenseCategory.INSURANCE_PREMIUM,
                    Expense.ExpenseCategory.OTHER
            );
        }
    }

    @Nested
    @DisplayName("Expense Calculations")
    class ExpenseCalculations {

        @Test
        @DisplayName("should calculate annual expense")
        void shouldCalculateAnnualExpense() {
            Expense expense = Expense.builder()
                    .monthlyAmount(25000.0)
                    .build();

            double annual = expense.getMonthlyAmount() * 12;
            assertThat(annual).isEqualTo(300000.0);
        }

        @Test
        @DisplayName("should distinguish fixed from variable")
        void shouldDistinguishFixedFromVariable() {
            Expense fixed = Expense.builder()
                    .name("Rent")
                    .category(Expense.ExpenseCategory.RENT)
                    .monthlyAmount(30000.0)
                    .isFixed(true)
                    .build();

            Expense variable = Expense.builder()
                    .name("Dining Out")
                    .category(Expense.ExpenseCategory.DINING)
                    .monthlyAmount(5000.0)
                    .isFixed(false)
                    .build();

            assertThat(fixed.getIsFixed()).isTrue();
            assertThat(variable.getIsFixed()).isFalse();
        }
    }
}
