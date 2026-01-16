package com.retyrment.service;

import com.retyrment.model.Insurance;
import com.retyrment.model.Investment;
import com.retyrment.model.RetirementScenario;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Insurance Type Branch Coverage Tests")
class RetirementServiceInsuranceBranchesTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private IncomeRepository incomeRepository;
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private RetirementScenarioRepository scenarioRepository;

    @InjectMocks
    private RetirementService retirementService;
    
    @Mock
    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.15);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
        
        // Setup default mocks
        when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
        when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
        when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
        when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
    }

    @Nested
    @DisplayName("Insurance Type - shouldContinueAfterRetirement Branches")
    class InsuranceContinueAfterRetirementBranches {

        @Test
        @DisplayName("should continue TERM_LIFE insurance after retirement")
        void shouldContinueTermLifeAfterRetirement() {
            Insurance termLife = Insurance.builder()
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .annualPremium(15000.0)
                    .policyName("Term Life Policy")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(termLife));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).hasSize(1);
            assertThat(continuingInsurance.get(0).get("type")).isEqualTo("TERM_LIFE");
        }

        @Test
        @DisplayName("should continue HEALTH PERSONAL insurance after retirement")
        void shouldContinueHealthPersonalAfterRetirement() {
            Insurance healthPersonal = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(Insurance.HealthInsuranceType.PERSONAL)
                    .annualPremium(20000.0)
                    .policyName("Health Personal")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(healthPersonal));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).hasSize(1);
        }

        @Test
        @DisplayName("should continue HEALTH FAMILY_FLOATER insurance after retirement")
        void shouldContinueHealthFamilyFloaterAfterRetirement() {
            Insurance healthFamily = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(Insurance.HealthInsuranceType.FAMILY_FLOATER)
                    .annualPremium(30000.0)
                    .policyName("Health Family")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(healthFamily));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).hasSize(1);
        }

        @Test
        @DisplayName("should NOT continue HEALTH GROUP insurance after retirement")
        void shouldNotContinueHealthGroupAfterRetirement() {
            Insurance healthGroup = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(Insurance.HealthInsuranceType.GROUP)
                    .annualPremium(25000.0)
                    .policyName("Health Group")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(healthGroup));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should NOT continue ULIP insurance after retirement")
        void shouldNotContinueUlipAfterRetirement() {
            Insurance ulip = Insurance.builder()
                    .type(Insurance.InsuranceType.ULIP)
                    .annualPremium(50000.0)
                    .policyName("ULIP Policy")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(ulip));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should NOT continue ENDOWMENT insurance after retirement")
        void shouldNotContinueEndowmentAfterRetirement() {
            Insurance endowment = Insurance.builder()
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .annualPremium(40000.0)
                    .policyName("Endowment Policy")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(endowment));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should NOT continue MONEY_BACK insurance after retirement")
        void shouldNotContinueMoneyBackAfterRetirement() {
            Insurance moneyBack = Insurance.builder()
                    .type(Insurance.InsuranceType.MONEY_BACK)
                    .annualPremium(35000.0)
                    .policyName("Money Back Policy")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(moneyBack));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should NOT continue VEHICLE insurance after retirement")
        void shouldNotContinueVehicleAfterRetirement() {
            Insurance vehicle = Insurance.builder()
                    .type(Insurance.InsuranceType.VEHICLE)
                    .annualPremium(15000.0)
                    .policyName("Vehicle Insurance")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(vehicle));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should NOT continue OTHER insurance after retirement")
        void shouldNotContinueOtherAfterRetirement() {
            Insurance other = Insurance.builder()
                    .type(Insurance.InsuranceType.OTHER)
                    .annualPremium(10000.0)
                    .policyName("Other Insurance")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(other));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should use explicitly set continuesAfterRetirement when provided")
        void shouldUseExplicitContinuesAfterRetirement() {
            Insurance policy = Insurance.builder()
                    .type(Insurance.InsuranceType.ULIP)
                    .continuesAfterRetirement(true) // Explicitly set to true
                    .annualPremium(50000.0)
                    .policyName("ULIP with explicit continue")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(policy));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).hasSize(1); // Should continue because explicitly set
        }

        @Test
        @DisplayName("should handle null insurance type")
        void shouldHandleNullInsuranceType() {
            Insurance policy = Insurance.builder()
                    .type(null)
                    .annualPremium(20000.0)
                    .policyName("Null Type Policy")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(policy));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            assertThat(continuingInsurance).isEmpty(); // Should not continue with null type
        }

        @Test
        @DisplayName("should handle null healthType for HEALTH insurance")
        void shouldHandleNullHealthType() {
            Insurance health = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(null) // Null health type
                    .annualPremium(20000.0)
                    .policyName("Health with null type")
                    .build();

            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of(health));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            // If healthType is null, it should default to continuing (PERSONAL/FAMILY_FLOATER path)
            assertThat(continuingInsurance).hasSize(1);
        }
    }
}
