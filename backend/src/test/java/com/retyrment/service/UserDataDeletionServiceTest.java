package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDataDeletionService Tests")
class UserDataDeletionServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private UserStrategyRepository userStrategyRepository;

    @Mock
    private RetirementScenarioRepository retirementScenarioRepository;

    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    @InjectMocks
    private UserDataDeletionService userDataDeletionService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
    }

    @Test
    @DisplayName("Get user data summary with data in all categories")
    void testGetUserDataSummary_WithData() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(createIncomeList(5));
        when(investmentRepository.findByUserId(testUserId)).thenReturn(createInvestmentList(10));
        when(loanRepository.findByUserId(testUserId)).thenReturn(createLoanList(2));
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(createInsuranceList(3));
        when(expenseRepository.findByUserId(testUserId)).thenReturn(createExpenseList(15));
        when(goalRepository.findByUserId(testUserId)).thenReturn(createGoalList(4));
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(createFamilyMemberList(4));
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(new UserPreference()));
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.of(new Settings()));
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.of(new UserStrategy()));
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> summary = userDataDeletionService.getUserDataSummary(testUserId);

        // Assert
        assertNotNull(summary);
        assertEquals(5, summary.get("income"));
        assertEquals(10, summary.get("investments"));
        assertEquals(2, summary.get("loans"));
        assertEquals(3, summary.get("insurance"));
        assertEquals(15, summary.get("expenses"));
        assertEquals(4, summary.get("goals"));
        assertEquals(4, summary.get("familyMembers"));
        assertEquals(1, summary.get("preferences"));
        assertEquals(1, summary.get("settings"));
        assertEquals(1, summary.get("strategies"));
        assertEquals(0, summary.get("scenarios"));
        assertEquals(0, summary.get("calendarEntries"));
        assertEquals(46, summary.get("total"));

        verify(incomeRepository).findByUserId(testUserId);
        verify(investmentRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Get user data summary with no data")
    void testGetUserDataSummary_WithNoData() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(investmentRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> summary = userDataDeletionService.getUserDataSummary(testUserId);

        // Assert
        assertNotNull(summary);
        assertEquals(0, summary.get("total"));
    }

    @Test
    @DisplayName("Delete all user data successfully")
    void testDeleteAllUserData_Success() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(createIncomeList(5));
        when(investmentRepository.findByUserId(testUserId)).thenReturn(createInvestmentList(10));
        when(loanRepository.findByUserId(testUserId)).thenReturn(createLoanList(2));
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(createInsuranceList(3));
        when(expenseRepository.findByUserId(testUserId)).thenReturn(createExpenseList(15));
        when(goalRepository.findByUserId(testUserId)).thenReturn(createGoalList(4));
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(createFamilyMemberList(4));
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(new UserPreference()));
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.of(new Settings()));
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.of(new UserStrategy()));
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> result = userDataDeletionService.deleteAllUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(46, result.get("total"));
        
        // Verify all deletions were called
        verify(incomeRepository).deleteByUserId(testUserId);
        verify(investmentRepository).deleteByUserId(testUserId);
        verify(loanRepository).deleteByUserId(testUserId);
        verify(insuranceRepository).deleteByUserId(testUserId);
        verify(expenseRepository).deleteByUserId(testUserId);
        verify(goalRepository).deleteByUserId(testUserId);
        verify(userPreferenceRepository).delete(any(UserPreference.class));
        verify(settingsRepository).delete(any(Settings.class));
        verify(userStrategyRepository).delete(any(UserStrategy.class));
        verify(retirementScenarioRepository).deleteByUserId(testUserId);
        verify(calendarEntryRepository).deleteByUserId(testUserId);
    }

    @Test
    @DisplayName("Delete all user data with empty data")
    void testDeleteAllUserData_WithEmptyData() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(investmentRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> result = userDataDeletionService.deleteAllUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(0, result.get("total"));

        // Verify deletions were still called (idempotent operation)
        verify(incomeRepository).deleteByUserId(testUserId);
        verify(investmentRepository).deleteByUserId(testUserId);
    }

    @Test
    @DisplayName("Delete handles exception gracefully")
    void testDeleteAllUserData_WithException() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(createIncomeList(5));
        doThrow(new RuntimeException("Database error")).when(incomeRepository).deleteByUserId(testUserId);

        // Act
        Map<String, Object> result = userDataDeletionService.deleteAllUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.get("success"));
        assertTrue(((String) result.get("error")).contains("Database error"));
    }

    @Test
    @DisplayName("Summary with retirement scenarios and calendar entries")
    void testGetUserDataSummary_WithRetirementScenariosAndCalendar() {
        // Arrange
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(investmentRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(retirementScenarioRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(new RetirementScenario(), new RetirementScenario()));
        when(calendarEntryRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(new CalendarEntry(), new CalendarEntry(), new CalendarEntry()));

        // Act
        Map<String, Object> summary = userDataDeletionService.getUserDataSummary(testUserId);

        // Assert
        assertNotNull(summary);
        assertEquals(2, summary.get("scenarios"));
        assertEquals(3, summary.get("calendarEntries"));
        assertEquals(5, summary.get("total"));
    }

    @Test
    @DisplayName("Delete with preferences, settings and strategies")
    void testDeleteAllUserData_WithPreferencesSettingsStrategies() {
        // Arrange
        UserPreference pref = new UserPreference();
        Settings settings = new Settings();
        UserStrategy strategy = new UserStrategy();
        
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(investmentRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(pref));
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.of(settings));
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.of(strategy));
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> result = userDataDeletionService.deleteAllUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(1, result.get("preferences"));
        assertEquals(1, result.get("settings"));
        assertEquals(1, result.get("strategies"));
        assertEquals(3, result.get("total"));
        
        verify(userPreferenceRepository).delete(pref);
        verify(settingsRepository).delete(settings);
        verify(userStrategyRepository).delete(strategy);
    }

    @Test
    @DisplayName("Delete family members one by one")
    void testDeleteAllUserData_DeletesFamilyMembersOneByOne() {
        // Arrange
        FamilyMember fm1 = new FamilyMember();
        fm1.setId("fm1");
        FamilyMember fm2 = new FamilyMember();
        fm2.setId("fm2");
        
        when(incomeRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(investmentRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(loanRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(insuranceRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(familyMemberRepository.findByUserId(testUserId)).thenReturn(Arrays.asList(fm1, fm2));
        when(userPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(userStrategyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(retirementScenarioRepository.findByUserId(testUserId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        Map<String, Object> result = userDataDeletionService.deleteAllUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(2, result.get("familyMembers"));
        
        verify(familyMemberRepository).deleteById("fm1");
        verify(familyMemberRepository).deleteById("fm2");
    }

    // Helper methods
    
    private List<Income> createIncomeList(int count) {
        Income[] incomes = new Income[count];
        for (int i = 0; i < count; i++) {
            incomes[i] = new Income();
            incomes[i].setId("income-" + i);
        }
        return Arrays.asList(incomes);
    }

    private List<Investment> createInvestmentList(int count) {
        Investment[] investments = new Investment[count];
        for (int i = 0; i < count; i++) {
            investments[i] = new Investment();
            investments[i].setId("investment-" + i);
        }
        return Arrays.asList(investments);
    }

    private List<Loan> createLoanList(int count) {
        Loan[] loans = new Loan[count];
        for (int i = 0; i < count; i++) {
            loans[i] = new Loan();
            loans[i].setId("loan-" + i);
        }
        return Arrays.asList(loans);
    }

    private List<Insurance> createInsuranceList(int count) {
        Insurance[] insurances = new Insurance[count];
        for (int i = 0; i < count; i++) {
            insurances[i] = new Insurance();
            insurances[i].setId("insurance-" + i);
        }
        return Arrays.asList(insurances);
    }

    private List<Expense> createExpenseList(int count) {
        Expense[] expenses = new Expense[count];
        for (int i = 0; i < count; i++) {
            expenses[i] = new Expense();
            expenses[i].setId("expense-" + i);
        }
        return Arrays.asList(expenses);
    }

    private List<Goal> createGoalList(int count) {
        Goal[] goals = new Goal[count];
        for (int i = 0; i < count; i++) {
            goals[i] = new Goal();
            goals[i].setId("goal-" + i);
        }
        return Arrays.asList(goals);
    }

    private List<FamilyMember> createFamilyMemberList(int count) {
        FamilyMember[] members = new FamilyMember[count];
        for (int i = 0; i < count; i++) {
            members[i] = new FamilyMember();
            members[i].setId("family-" + i);
        }
        return Arrays.asList(members);
    }
}
