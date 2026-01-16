package com.retyrment.service;

import com.retyrment.model.Insurance;
import com.retyrment.model.Investment;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Maturing Before Retirement Branch Coverage Tests")
class RetirementServiceMaturingTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private RetirementService retirementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
    }

    @Nested
    @DisplayName("calculateMaturingBeforeRetirement - Investment Branches")
    class InvestmentMaturingBranches {

        @Test
        @DisplayName("should include investment with maturity date before retirement")
        void shouldIncludeInvestmentWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10); // 10 years from now, before retirement at 60

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(maturityDate)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should exclude investment with maturity date after retirement")
        void shouldExcludeInvestmentWithMaturityAfterRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(30); // 30 years from now, after retirement at 60

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(maturityDate)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should exclude investment with null maturity date")
        void shouldExcludeInvestmentWithNullMaturityDate() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Mutual Fund")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(null)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(mf));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should exclude investment with maturity date in the past")
        void shouldExcludeInvestmentWithMaturityInPast() {
            LocalDate today = LocalDate.now();
            LocalDate pastMaturity = today.minusYears(1); // Already matured

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(pastMaturity)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should handle investment with null type")
        void shouldHandleInvestmentWithNullType() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(null)
                    .maturityDate(maturityDate)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(inv));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("should handle investment with null currentValue")
        void shouldHandleInvestmentWithNullCurrentValue() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(maturityDate)
                    .currentValue(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("currentValue")).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("calculateMaturingBeforeRetirement - Insurance Branches")
    class InsuranceMaturingBranches {

        @Test
        @DisplayName("should include ULIP with maturity date before retirement")
        void shouldIncludeUlipWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(200000.0)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(300000.0);

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("type")).isEqualTo("ULIP");
        }

        @Test
        @DisplayName("should include ENDOWMENT with maturity date before retirement")
        void shouldIncludeEndowmentWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance endowment = Insurance.builder()
                    .id("end1")
                    .policyName("Endowment Policy")
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .maturityDate(maturityDate)
                    .sumAssured(1000000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(endowment));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("type")).isEqualTo("ENDOWMENT");
        }

        @Test
        @DisplayName("should include MONEY_BACK with maturity date before retirement")
        void shouldIncludeMoneyBackWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance moneyBack = Insurance.builder()
                    .id("mb1")
                    .policyName("Money Back Policy")
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .maturityDate(maturityDate)
                    .sumAssured(800000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(moneyBack));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("type")).isEqualTo("MONEY_BACK");
        }

        @Test
        @DisplayName("should exclude TERM_LIFE insurance (no maturity)")
        void shouldExcludeTermLifeInsurance() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance termLife = Insurance.builder()
                    .id("term1")
                    .policyName("Term Life")
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .maturityDate(maturityDate)
                    .sumAssured(5000000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(termLife));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should exclude HEALTH insurance (no maturity)")
        void shouldExcludeHealthInsurance() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance health = Insurance.builder()
                    .id("health1")
                    .policyName("Health Insurance")
                    .type(Insurance.InsuranceType.HEALTH)
                    .maturityDate(maturityDate)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(health));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should use fundValue for ULIP when available")
        void shouldUseFundValueForUlipWhenAvailable() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(200000.0)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));
            when(calculationService.calculateFutureValue(200000.0, 8.0, 10))
                    .thenReturn(400000.0);

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            // Should use calculated fund value, not sum assured
            assertThat(maturingInsurance.get(0).get("expectedMaturityValue")).isEqualTo(400000L);
        }

        @Test
        @DisplayName("should use sumAssured for ULIP when fundValue is null")
        void shouldUseSumAssuredForUlipWhenFundValueIsNull() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(null)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            // Should use sum assured when fund value is null
            assertThat(maturingInsurance.get(0).get("expectedMaturityValue")).isEqualTo(500000L);
        }

        @Test
        @DisplayName("should handle insurance with null sumAssured")
        void shouldHandleInsuranceWithNullSumAssured() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance endowment = Insurance.builder()
                    .id("end1")
                    .policyName("Endowment Policy")
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .maturityDate(maturityDate)
                    .sumAssured(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(endowment));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("expectedMaturityValue")).isEqualTo(0L);
        }

        @Test
        @DisplayName("should handle insurance with null fundValue")
        void shouldHandleInsuranceWithNullFundValue() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(null)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("currentFundValue")).isEqualTo(0L);
        }

        @Test
        @DisplayName("should exclude insurance with maturity date after retirement")
        void shouldExcludeInsuranceWithMaturityAfterRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(30); // After retirement

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should exclude insurance with null maturity date")
        void shouldExcludeInsuranceWithNullMaturityDate() {
            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(null)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).isEmpty();
        }
    }
}
