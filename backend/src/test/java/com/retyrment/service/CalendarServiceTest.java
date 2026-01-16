package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalendarServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    @InjectMocks
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        when(calendarEntryRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(loanRepository.findByUserIdAndRemainingMonthsGreaterThan("test-user", 0)).thenReturn(Collections.emptyList());
        when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("generateYearCalendar")
    class GenerateYearCalendar {

        @Test
        @DisplayName("should return empty calendar when no data")
        void shouldReturnEmptyCalendar() {
            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            assertThat(result).containsKeys("entries", "monthlyTotals", "yearlyGrandTotal", "year");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).isEmpty();
            assertThat(((Number) result.get("yearlyGrandTotal")).doubleValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("should include SIPs from mutual funds")
        void shouldIncludeSips() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Axis Bluechip")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .sipDay(5)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("description")).isEqualTo("Axis Bluechip SIP");
            assertThat(entries.get(0).get("category")).isEqualTo("SIP");
            assertThat(((Number) entries.get(0).get("yearlyTotal")).doubleValue()).isEqualTo(120000.0);
        }

        @Test
        @DisplayName("should include RDs with correct category")
        void shouldIncludeRds() {
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("HDFC RD")
                    .type(Investment.InvestmentType.RD)
                    .monthlySip(5000.0)
                    .rdDay(15)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(rd));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("category")).isEqualTo("RD");
        }

        @Test
        @DisplayName("should include EMIs from active loans")
        void shouldIncludeEmis() {
            Loan loan = Loan.builder()
                    .id("loan1")
                    .name("Home Loan SBI")
                    .type(Loan.LoanType.HOME)
                    .emi(50000.0)
                    .remainingMonths(120)
                    .build();

            when(loanRepository.findByUserIdAndRemainingMonthsGreaterThan("test-user", 0)).thenReturn(Arrays.asList(loan));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("category")).isEqualTo("EMI");
            assertThat(((Number) entries.get(0).get("yearlyTotal")).doubleValue()).isEqualTo(600000.0);
        }

        @Test
        @DisplayName("should include PPF yearly contributions")
        void shouldIncludePpfContributions() {
            Investment ppf = Investment.builder()
                    .id("ppf1")
                    .name("PPF Account")
                    .type(Investment.InvestmentType.PPF)
                    .yearlyContribution(150000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ppf));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("category")).isEqualTo("PPF");
        }

        @Test
        @DisplayName("should calculate monthly totals correctly")
        void shouldCalculateMonthlyTotals() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("MF")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .build();

            Loan loan = Loan.builder()
                    .id("loan1")
                    .name("Loan")
                    .emi(20000.0)
                    .remainingMonths(12)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));
            when(loanRepository.findByUserIdAndRemainingMonthsGreaterThan("test-user", 0)).thenReturn(Arrays.asList(loan));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            Map<String, Double> monthlyTotals = (Map<String, Double>) result.get("monthlyTotals");
            
            assertThat(monthlyTotals.get("JAN")).isEqualTo(30000.0);
            assertThat(((Number) result.get("yearlyGrandTotal")).doubleValue()).isEqualTo(360000.0);
        }
    }

    @Nested
    @DisplayName("getMonthCalendar")
    class GetMonthCalendar {

        @Test
        @DisplayName("should return entries for specific month")
        void shouldReturnEntriesForMonth() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("MF")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .sipDay(5)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));

            Map<String, Object> result = calendarService.getMonthCalendar("test-user", 1);

            assertThat(result.get("month")).isEqualTo(1);
            assertThat(result.get("monthName")).isEqualTo("JAN");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("amount")).isEqualTo(10000.0);
        }

        @Test
        @DisplayName("should return correct total for month")
        void shouldReturnCorrectTotal() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("MF")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(15000.0)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));

            Map<String, Object> result = calendarService.getMonthCalendar("test-user", 6);

            assertThat(result.get("total")).isEqualTo(15000.0);
        }
    }

    @Nested
    @DisplayName("getUpcomingPayments")
    class GetUpcomingPayments {

        @Test
        @DisplayName("should return entries for current and next month")
        void shouldReturnUpcomingPayments() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("MF")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));

            List<Map<String, Object>> result = calendarService.getUpcomingPayments("test-user", 30);

            assertThat(result).hasSize(2);
            
            long currentMonthEntries = result.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("isCurrentMonth")))
                    .count();
            assertThat(currentMonthEntries).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Insurance Premium Branches")
    class InsurancePremiumBranches {

        @Test
        @DisplayName("should handle yearly premium frequency")
        void shouldHandleYearlyPremium() {
            Insurance health = Insurance.builder()
                    .id("ins1")
                    .policyName("Health Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.YEARLY)
                    .annualPremium(24000.0)
                    .renewalMonth(3)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(health));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Yearly premium should only appear in renewal month (March = index 2)
            assertThat(months.get("MAR")).isEqualTo(24000.0);
            assertThat(months.get("JAN")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle monthly premium frequency")
        void shouldHandleMonthlyPremium() {
            Insurance term = Insurance.builder()
                    .id("ins1")
                    .policyName("Term Insurance")
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .premiumFrequency(Insurance.PremiumFrequency.MONTHLY)
                    .annualPremium(12000.0)
                    .renewalMonth(1)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(term));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Monthly premium should be 1000 (12000/12) in all months
            assertThat(months.get("JAN")).isEqualTo(1000.0);
            assertThat(months.get("DEC")).isEqualTo(1000.0);
        }

        @Test
        @DisplayName("should handle insurance without renewal month")
        void shouldHandleInsuranceWithoutRenewalMonth() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.YEARLY)
                    .annualPremium(24000.0)
                    .renewalMonth(null)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(insurance));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
        }

        @Test
        @DisplayName("should handle half-yearly premium frequency")
        void shouldHandleHalfYearlyPremium() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("Half Yearly Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.HALF_YEARLY)
                    .annualPremium(24000.0)
                    .renewalMonth(3) // March (index 2)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(insurance));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Half-yearly premium should appear in March (index 2) and September (index 8)
            assertThat(months.get("MAR")).isEqualTo(12000.0); // 24000/2
            assertThat(months.get("SEP")).isEqualTo(12000.0); // (2+6)%12 = 8
            assertThat(months.get("JAN")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle quarterly premium frequency")
        void shouldHandleQuarterlyPremium() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("Quarterly Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.QUARTERLY)
                    .annualPremium(24000.0)
                    .renewalMonth(1) // January (index 0)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(insurance));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Quarterly premium should appear in Jan, Apr, Jul, Oct (indices 0, 3, 6, 9)
            assertThat(months.get("JAN")).isEqualTo(6000.0); // 24000/4
            assertThat(months.get("APR")).isEqualTo(6000.0); // (0+3)%12 = 3
            assertThat(months.get("JUL")).isEqualTo(6000.0); // (0+6)%12 = 6
            assertThat(months.get("OCT")).isEqualTo(6000.0); // (0+9)%12 = 9
            assertThat(months.get("FEB")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle half-yearly premium with renewal month crossing year boundary")
        void shouldHandleHalfYearlyPremiumCrossingBoundary() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("Half Yearly Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.HALF_YEARLY)
                    .annualPremium(24000.0)
                    .renewalMonth(10) // October (index 9)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(insurance));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Half-yearly premium should appear in October (index 9) and April (index 3) = (9+6)%12
            assertThat(months.get("OCT")).isEqualTo(12000.0);
            assertThat(months.get("APR")).isEqualTo(12000.0);
        }

        @Test
        @DisplayName("should handle quarterly premium with renewal month crossing boundaries")
        void shouldHandleQuarterlyPremiumCrossingBoundaries() {
            Insurance insurance = Insurance.builder()
                    .id("ins1")
                    .policyName("Quarterly Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .premiumFrequency(Insurance.PremiumFrequency.QUARTERLY)
                    .annualPremium(24000.0)
                    .renewalMonth(11) // November (index 10)
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(insurance));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Quarterly premium should appear in Nov, Feb, May, Aug (indices 10, 1, 4, 7)
            assertThat(months.get("NOV")).isEqualTo(6000.0);
            assertThat(months.get("FEB")).isEqualTo(6000.0); // (10+3)%12 = 1
            assertThat(months.get("MAY")).isEqualTo(6000.0); // (10+6)%12 = 4
            assertThat(months.get("AUG")).isEqualTo(6000.0); // (10+9)%12 = 7
        }

        @Test
        @DisplayName("should handle RD without rdDay using sipDay")
        void shouldHandleRdWithoutRdDay() {
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("RD")
                    .type(Investment.InvestmentType.RD)
                    .monthlySip(5000.0)
                    .rdDay(null)
                    .sipDay(10)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(rd));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).get("sipDay")).isEqualTo(10);
        }

        @Test
        @DisplayName("should handle loan with less than 12 months remaining")
        void shouldHandleLoanWithFewMonthsRemaining() {
            Loan loan = Loan.builder()
                    .id("loan1")
                    .name("Loan")
                    .type(Loan.LoanType.HOME)
                    .emi(50000.0)
                    .remainingMonths(6) // Less than 12
                    .build();

            when(loanRepository.findByUserIdAndRemainingMonthsGreaterThan("test-user", 0)).thenReturn(Arrays.asList(loan));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries).hasSize(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entries.get(0).get("months");
            // Should only have 6 months of EMI
            long monthsWithEmi = months.values().stream().filter(v -> v > 0).count();
            assertThat(monthsWithEmi).isEqualTo(6);
        }

        @Test
        @DisplayName("should handle investment without sipDay defaulting to 1")
        void shouldHandleInvestmentWithoutSipDay() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("MF")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .sipDay(null)
                    .build();

            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0)).thenReturn(Arrays.asList(mf));

            Map<String, Object> result = calendarService.generateYearCalendar("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
            assertThat(entries.get(0).get("sipDay")).isEqualTo(1);
        }
    }
}
