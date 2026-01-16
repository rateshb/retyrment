package com.retyrment.service;

import com.retyrment.model.Loan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalculationServiceTest {

    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new CalculationService();
    }

    @Nested
    @DisplayName("SIP Future Value Calculation")
    class SipFutureValue {

        @Test
        @DisplayName("should calculate SIP future value correctly")
        void shouldCalculateSipFV() {
            // ₹10,000/month for 10 years at 12% p.a.
            double fv = calculationService.calculateSIPFutureValue(10000, 12.0, 10);
            
            // Expected approximately ₹23,23,391 (may vary slightly based on formula)
            assertThat(fv).isCloseTo(2323391, within(50000.0));
        }

        @Test
        @DisplayName("should return 0 for zero SIP")
        void shouldReturnZeroForZeroSip() {
            double fv = calculationService.calculateSIPFutureValue(0, 12.0, 10);
            assertThat(fv).isEqualTo(0);
        }

        @Test
        @DisplayName("should return 0 for zero years")
        void shouldReturnZeroForZeroYears() {
            double fv = calculationService.calculateSIPFutureValue(10000, 12.0, 0);
            assertThat(fv).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Lumpsum Future Value Calculation")
    class LumpsumFutureValue {

        @Test
        @DisplayName("should calculate lumpsum future value correctly")
        void shouldCalculateLumpsumFV() {
            // ₹1,00,000 for 10 years at 12% p.a.
            double fv = calculationService.calculateFutureValue(100000, 12.0, 10);
            
            // Expected: 100000 * (1.12)^10 ≈ 310,584
            assertThat(fv).isCloseTo(310584, within(100.0));
        }

        @Test
        @DisplayName("should return principal for zero years")
        void shouldReturnPrincipalForZeroYears() {
            double fv = calculationService.calculateFutureValue(100000, 12.0, 0);
            assertThat(fv).isCloseTo(100000, within(1.0));
        }
        
        @Test
        @DisplayName("should handle zero interest rate")
        void shouldHandleZeroInterestRate() {
            double fv = calculationService.calculateFutureValue(100000, 0.0, 10);
            assertThat(fv).isCloseTo(100000, within(1.0));
        }
    }

    @Nested
    @DisplayName("Loan Amortization")
    class LoanAmortization {

        @Test
        @DisplayName("should calculate amortization schedule correctly")
        void shouldCalculateAmortization() {
            Loan loan = Loan.builder()
                    .originalAmount(5000000.0)
                    .outstandingAmount(5000000.0)
                    .interestRate(8.5)
                    .emi(45000.0)
                    .tenureMonths(180)
                    .remainingMonths(180)
                    .startDate(LocalDate.now())
                    .build();

            List<Map<String, Object>> schedule = calculationService.calculateAmortization(loan);

            assertThat(schedule).isNotEmpty();
            
            // First month's interest should be: Principal * Rate / 12
            Map<String, Object> firstMonth = schedule.get(0);
            Long interest = (Long) firstMonth.get("interest");
            double expectedInterest = 5000000 * 0.085 / 12;
            assertThat(interest.doubleValue()).isCloseTo(expectedInterest, within(10.0));
        }

        @Test
        @DisplayName("should show decreasing balance over time")
        void shouldShowDecreasingBalance() {
            Loan loan = Loan.builder()
                    .originalAmount(1000000.0)
                    .outstandingAmount(1000000.0)
                    .interestRate(10.0)
                    .emi(15000.0)
                    .tenureMonths(120)
                    .remainingMonths(120)
                    .startDate(LocalDate.now())
                    .build();

            List<Map<String, Object>> schedule = calculationService.calculateAmortization(loan);

            if (schedule.size() >= 2) {
                Long firstBalance = (Long) schedule.get(0).get("balance");
                Long secondBalance = (Long) schedule.get(1).get("balance");
                assertThat(secondBalance).isLessThan(firstBalance);
            }
        }
    }

    @Nested
    @DisplayName("Inflation Adjustment")
    class InflationAdjustment {

        @Test
        @DisplayName("should calculate inflation-adjusted amount")
        void shouldCalculateInflatedAmount() {
            double inflated = calculationService.calculateInflatedValue(100000, 6.0, 10);
            
            // Expected: 100000 * (1.06)^10 ≈ 179,084
            assertThat(inflated).isCloseTo(179084, within(100.0));
        }

        @Test
        @DisplayName("should return original amount for zero years")
        void shouldReturnOriginalForZeroYears() {
            double inflated = calculationService.calculateInflatedValue(100000, 6.0, 0);
            assertThat(inflated).isCloseTo(100000, within(1.0));
        }
    }

    @Nested
    @DisplayName("Required SIP Calculation")
    class RequiredSip {

        @Test
        @DisplayName("should calculate required SIP for goal")
        void shouldCalculateRequiredSip() {
            // Target: ₹1 crore in 10 years at 12% return
            double requiredSip = calculationService.calculateRequiredSIP(10000000, 12.0, 10);
            
            // Should be around ₹43,000/month
            assertThat(requiredSip).isBetween(40000.0, 50000.0);
        }
        
        @Test
        @DisplayName("should return target amount for zero years")
        void shouldReturnTargetForZeroYears() {
            double requiredSip = calculationService.calculateRequiredSIP(100000, 12.0, 0);
            assertThat(requiredSip).isCloseTo(100000, within(1.0));
        }
    }
    
    @Nested
    @DisplayName("EMI Calculation")
    class EmiCalculation {
        
        @Test
        @DisplayName("should calculate EMI correctly")
        void shouldCalculateEmi() {
            // ₹50 lakh loan at 8.5% for 20 years
            double emi = calculationService.calculateEMI(5000000, 8.5, 240);
            
            // Expected EMI around ₹43,391
            assertThat(emi).isCloseTo(43391, within(100.0));
        }
    }
    
    @Nested
    @DisplayName("CAGR Calculation")
    class CagrCalculation {
        
        @Test
        @DisplayName("should calculate CAGR correctly")
        void shouldCalculateCagr() {
            // ₹1 lakh grew to ₹3 lakh in 10 years
            double cagr = calculationService.calculateCAGR(100000, 300000, 10);
            
            // Expected CAGR around 11.6%
            assertThat(cagr).isCloseTo(11.6, within(0.2));
        }
        
        @Test
        @DisplayName("should return 0 for zero years")
        void shouldReturnZeroForZeroYears() {
            double cagr = calculationService.calculateCAGR(100000, 200000, 0);
            assertThat(cagr).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Absolute Returns Calculation")
    class AbsoluteReturns {
        
        @Test
        @DisplayName("should calculate absolute returns correctly")
        void shouldCalculateAbsoluteReturns() {
            double returns = calculationService.calculateAbsoluteReturns(100000, 150000);
            assertThat(returns).isCloseTo(50.0, within(0.1));
        }
        
        @Test
        @DisplayName("should handle negative returns")
        void shouldHandleNegativeReturns() {
            double returns = calculationService.calculateAbsoluteReturns(100000, 80000);
            assertThat(returns).isCloseTo(-20.0, within(0.1));
        }
        
        @Test
        @DisplayName("should return 0 for zero investment")
        void shouldReturnZeroForZeroInvestment() {
            double returns = calculationService.calculateAbsoluteReturns(0, 50000);
            assertThat(returns).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Step-up SIP Future Value")
    class StepUpSip {
        
        @Test
        @DisplayName("should calculate step-up SIP future value")
        void shouldCalculateStepUpSipFV() {
            // ₹10,000/month with 10% step-up for 10 years at 12%
            double fv = calculationService.calculateStepUpSIPFutureValue(10000, 12.0, 10.0, 10);
            
            // Should be higher than regular SIP
            double regularSipFV = calculationService.calculateSIPFutureValue(10000, 12.0, 10);
            assertThat(fv).isGreaterThan(regularSipFV);
        }
    }
    
    @Nested
    @DisplayName("PPF Maturity Calculation")
    class PpfMaturity {
        
        @Test
        @DisplayName("should calculate PPF maturity value")
        void shouldCalculatePpfMaturity() {
            // Current balance ₹5 lakh, ₹1.5 lakh yearly contribution, 7.1% rate, 10 years
            double maturity = calculationService.calculatePPFMaturity(500000, 150000, 7.1, 10);
            
            // Should grow significantly with compounding
            assertThat(maturity).isGreaterThan(500000 + (150000 * 10));
        }
    }
}
