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

class InvestmentTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build valid mutual fund investment")
        void shouldBuildMutualFund() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Axis Bluechip")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(500000.0)
                    .investedAmount(400000.0)
                    .monthlySip(10000.0)
                    .expectedReturn(12.0)
                    .sipDay(5)
                    .build();

            assertThat(mf.getName()).isEqualTo("Axis Bluechip");
            assertThat(mf.getType()).isEqualTo(Investment.InvestmentType.MUTUAL_FUND);
            assertThat(mf.getCurrentValue()).isEqualTo(500000.0);
            assertThat(mf.getSipDay()).isEqualTo(5);
        }

        @Test
        @DisplayName("should build PPF investment with yearly contribution")
        void shouldBuildPpfInvestment() {
            Investment ppf = Investment.builder()
                    .name("PPF Account")
                    .type(Investment.InvestmentType.PPF)
                    .currentValue(300000.0)
                    .yearlyContribution(150000.0)
                    .interestRate(7.1)
                    .maturityDate(LocalDate.of(2035, 4, 1))
                    .build();

            assertThat(ppf.getType()).isEqualTo(Investment.InvestmentType.PPF);
            assertThat(ppf.getYearlyContribution()).isEqualTo(150000.0);
        }

        @Test
        @DisplayName("should build RD with rdDay")
        void shouldBuildRd() {
            Investment rd = Investment.builder()
                    .name("HDFC RD")
                    .type(Investment.InvestmentType.RD)
                    .monthlySip(5000.0)
                    .rdDay(15)
                    .maturityDate(LocalDate.of(2027, 1, 15))
                    .build();

            assertThat(rd.getType()).isEqualTo(Investment.InvestmentType.RD);
            assertThat(rd.getRdDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("should build FD investment")
        void shouldBuildFd() {
            Investment fd = Investment.builder()
                    .name("SBI FD")
                    .type(Investment.InvestmentType.FD)
                    .investedAmount(500000.0)
                    .currentValue(500000.0)
                    .interestRate(6.5)
                    .tenureMonths(24)
                    .maturityDate(LocalDate.now().plusMonths(24))
                    .build();

            assertThat(fd.getType()).isEqualTo(Investment.InvestmentType.FD);
            assertThat(fd.getInterestRate()).isEqualTo(6.5);
        }

        @Test
        @DisplayName("should build stock investment")
        void shouldBuildStock() {
            Investment stock = Investment.builder()
                    .name("Reliance Industries")
                    .type(Investment.InvestmentType.STOCK)
                    .investedAmount(100000.0)
                    .currentValue(120000.0)
                    .purchaseDate(LocalDate.now().minusMonths(12))
                    .build();

            assertThat(stock.getType()).isEqualTo(Investment.InvestmentType.STOCK);
        }

        @Test
        @DisplayName("should build NPS investment")
        void shouldBuildNps() {
            Investment nps = Investment.builder()
                    .name("NPS Tier 1")
                    .type(Investment.InvestmentType.NPS)
                    .currentValue(200000.0)
                    .monthlySip(5000.0)
                    .expectedReturn(10.0)
                    .build();

            assertThat(nps.getType()).isEqualTo(Investment.InvestmentType.NPS);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameBlank() {
            Investment inv = Investment.builder()
                    .name("")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .build();

            Set<ConstraintViolation<Investment>> violations = validator.validate(inv);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("should fail when type is null")
        void shouldFailWhenTypeNull() {
            Investment inv = Investment.builder()
                    .name("Test Investment")
                    .type(null)
                    .build();

            Set<ConstraintViolation<Investment>> violations = validator.validate(inv);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when currentValue is negative")
        void shouldFailWhenCurrentValueNegative() {
            Investment inv = Investment.builder()
                    .name("Test")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(-1000.0)
                    .build();

            Set<ConstraintViolation<Investment>> violations = validator.validate(inv);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should fail when SIP day is out of range")
        void shouldFailWhenSipDayOutOfRange() {
            Investment inv = Investment.builder()
                    .name("Test")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .sipDay(30)
                    .build();

            Set<ConstraintViolation<Investment>> violations = validator.validate(inv);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should pass validation for valid investment")
        void shouldPassForValidInvestment() {
            Investment inv = Investment.builder()
                    .name("Valid Investment")
                    .type(Investment.InvestmentType.FD)
                    .currentValue(100000.0)
                    .investedAmount(100000.0)
                    .interestRate(6.5)
                    .build();

            Set<ConstraintViolation<Investment>> violations = validator.validate(inv);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Investment Types")
    class InvestmentTypes {

        @Test
        @DisplayName("should have all expected types")
        void shouldHaveAllTypes() {
            assertThat(Investment.InvestmentType.values()).containsExactlyInAnyOrder(
                    Investment.InvestmentType.MUTUAL_FUND,
                    Investment.InvestmentType.STOCK,
                    Investment.InvestmentType.FD,
                    Investment.InvestmentType.RD,
                    Investment.InvestmentType.PPF,
                    Investment.InvestmentType.EPF,
                    Investment.InvestmentType.NPS,
                    Investment.InvestmentType.GOLD,
                    Investment.InvestmentType.REAL_ESTATE,
                    Investment.InvestmentType.CRYPTO,
                    Investment.InvestmentType.CASH,
                    Investment.InvestmentType.OTHER
            );
        }
    }

    @Nested
    @DisplayName("Returns Calculation")
    class ReturnsCalculation {

        @Test
        @DisplayName("should calculate absolute returns")
        void shouldCalculateAbsoluteReturns() {
            Investment inv = Investment.builder()
                    .investedAmount(100000.0)
                    .currentValue(120000.0)
                    .build();

            double absoluteReturns = ((inv.getCurrentValue() - inv.getInvestedAmount()) / inv.getInvestedAmount()) * 100;
            assertThat(absoluteReturns).isEqualTo(20.0);
        }

        @Test
        @DisplayName("should calculate profit/loss")
        void shouldCalculateProfitLoss() {
            Investment profit = Investment.builder()
                    .investedAmount(100000.0)
                    .currentValue(150000.0)
                    .build();

            Investment loss = Investment.builder()
                    .investedAmount(100000.0)
                    .currentValue(80000.0)
                    .build();

            double profitAmount = profit.getCurrentValue() - profit.getInvestedAmount();
            double lossAmount = loss.getCurrentValue() - loss.getInvestedAmount();

            assertThat(profitAmount).isEqualTo(50000.0);
            assertThat(lossAmount).isEqualTo(-20000.0);
        }
    }
}
