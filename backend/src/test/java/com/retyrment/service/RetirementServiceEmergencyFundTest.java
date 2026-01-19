package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for Emergency Fund Tagging feature in RetirementService
 * Tests the functionality that excludes FDs/RDs tagged as emergency funds from retirement corpus
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetirementService Emergency Fund Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RetirementServiceEmergencyFundTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private RetirementScenarioRepository scenarioRepository;

    @Mock
    private CalculationService calculationService;

    private RetirementService retirementService;
    private String testUserId;
    private RetirementScenario defaultScenario;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        
        // Create the service with all dependencies (order matches RetirementService constructor)
        retirementService = new RetirementService(
            investmentRepository, 
            insuranceRepository, 
            goalRepository,
            expenseRepository, 
            incomeRepository, 
            loanRepository, 
            scenarioRepository,
            calculationService
        );
        
        // Create default scenario
        defaultScenario = new RetirementScenario();
        defaultScenario.setId("scenario-1");
        defaultScenario.setUserId(testUserId);
        defaultScenario.setCurrentAge(35);
        defaultScenario.setRetirementAge(60);
        defaultScenario.setLifeExpectancy(85);
        defaultScenario.setInflationRate(6.0);
        defaultScenario.setCorpusReturnRate(10.0);
        defaultScenario.setSipStepUpPercent(10.0);
        defaultScenario.setIsDefault(true);
    }

    @Test
    @DisplayName("Retirement matrix excludes emergency FDs from corpus")
    void testRetirementMatrix_ExcludesEmergencyFDs() {
        // Arrange - Regular FD and Emergency FD
        Investment regularFD = createFD("FD1", 100000.0, 110000.0, false);
        Investment emergencyFD = createFD("FD2", 200000.0, 220000.0, true);

        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.FD)))
                .thenReturn(Arrays.asList(regularFD, emergencyFD));
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.RD)))
                .thenReturn(List.of());
        setupEmptyRepositories();
        when(scenarioRepository.findByUserIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(defaultScenario));
        when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

        // Act
        Map<String, Object> matrix = retirementService.generateRetirementMatrix(testUserId, null);

        // Assert
        assertNotNull(matrix);
        Map<String, Object> summary = (Map<String, Object>) matrix.get("summary");
        assertNotNull(summary);
        
        Map<String, Object> startingBalances = (Map<String, Object>) summary.get("startingBalances");
        assertNotNull(startingBalances);
        
        // Emergency FD should be reported separately
        Long emergencyFund = ((Number) startingBalances.get("emergencyFund")).longValue();
        assertEquals(220000L, emergencyFund, "Emergency fund should equal the emergency FD value");
    }

    @Test
    @DisplayName("Retirement matrix includes both FD and RD emergency funds")
    void testRetirementMatrix_IncludesBothFDAndRDEmergencyFunds() {
        // Arrange
        Investment emergencyFD = createFD("FD1", 100000.0, 110000.0, true);
        Investment emergencyRD = createRD("RD1", 50000.0, 55000.0, true);

        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.FD)))
                .thenReturn(List.of(emergencyFD));
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.RD)))
                .thenReturn(List.of(emergencyRD));
        setupEmptyRepositories();
        when(scenarioRepository.findByUserIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(defaultScenario));
        when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

        // Act
        Map<String, Object> matrix = retirementService.generateRetirementMatrix(testUserId, null);

        // Assert
        assertNotNull(matrix);
        Map<String, Object> summary = (Map<String, Object>) matrix.get("summary");
        Map<String, Object> startingBalances = (Map<String, Object>) summary.get("startingBalances");
        
        Long emergencyFund = ((Number) startingBalances.get("emergencyFund")).longValue();
        assertEquals(165000L, emergencyFund, "Emergency fund should sum both FD and RD");
    }

    @Test
    @DisplayName("Retirement matrix with no emergency funds")
    void testRetirementMatrix_NoEmergencyFunds() {
        // Arrange - All FDs are regular (not tagged as emergency)
        Investment regularFD1 = createFD("FD1", 100000.0, 110000.0, false);
        Investment regularFD2 = createFD("FD2", 200000.0, 220000.0, false);

        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.FD)))
                .thenReturn(Arrays.asList(regularFD1, regularFD2));
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.RD)))
                .thenReturn(List.of());
        setupEmptyRepositories();
        when(scenarioRepository.findByUserIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(defaultScenario));
        when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

        // Act
        Map<String, Object> matrix = retirementService.generateRetirementMatrix(testUserId, null);

        // Assert
        assertNotNull(matrix);
        Map<String, Object> summary = (Map<String, Object>) matrix.get("summary");
        Map<String, Object> startingBalances = (Map<String, Object>) summary.get("startingBalances");
        
        Long emergencyFund = ((Number) startingBalances.get("emergencyFund")).longValue();
        assertEquals(0L, emergencyFund, "Emergency fund should be 0 when no FDs/RDs are tagged");
    }

    // Helper methods
    
    private void setupEmptyRepositories() {
        // Setup other investment types as empty
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.PPF)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.EPF)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.MUTUAL_FUND)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.NPS)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.STOCK)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.CASH)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.GOLD)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.REAL_ESTATE)))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndType(eq(testUserId), eq(Investment.InvestmentType.CRYPTO)))
                .thenReturn(List.of());
        
        // Setup other repositories as empty
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
    }

    private Investment createFD(String id, double invested, double current, boolean isEmergency) {
        Investment fd = new Investment();
        fd.setId(id);
        fd.setUserId(testUserId);
        fd.setType(Investment.InvestmentType.FD);
        fd.setName("FD " + id);
        fd.setInvestedAmount(invested);
        fd.setCurrentValue(current);
        fd.setIsEmergencyFund(isEmergency);
        return fd;
    }

    private Investment createRD(String id, double invested, double current, boolean isEmergency) {
        Investment rd = new Investment();
        rd.setId(id);
        rd.setUserId(testUserId);
        rd.setType(Investment.InvestmentType.RD);
        rd.setName("RD " + id);
        rd.setInvestedAmount(invested);
        rd.setCurrentValue(current);
        rd.setIsEmergencyFund(isEmergency);
        return rd;
    }
}
