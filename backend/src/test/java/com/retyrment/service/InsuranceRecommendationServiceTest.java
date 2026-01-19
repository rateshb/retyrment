package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import com.retyrment.service.InsuranceRecommendationService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for InsuranceRecommendationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceRecommendationService")
class InsuranceRecommendationServiceTest {

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private InsuranceRecommendationService service;

    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        // Default settings
        Settings settings = Settings.builder()
                .userId(USER_ID)
                .currentAge(35)
                .retirementAge(60)
                .build();
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
    }

    @Nested
    @DisplayName("Health Insurance Recommendations")
    class HealthInsuranceRecommendations {

        @Test
        @DisplayName("should recommend base health cover for individual")
        void shouldRecommendBaseHealthCoverForIndividual() {
            // Setup: Single user with no family
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getHealthRecommendation()).isNotNull();
            assertThat(result.getHealthRecommendation().getTotalRecommendedCover()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should recommend higher cover for family with children")
        void shouldRecommendHigherCoverForFamilyWithChildren() {
            List<FamilyMember> family = Arrays.asList(
                    FamilyMember.builder()
                            .name("Self")
                            .relationship(FamilyMember.Relationship.SELF)
                            .dateOfBirth(LocalDate.now().minusYears(35))
                            .build(),
                    FamilyMember.builder()
                            .name("Spouse")
                            .relationship(FamilyMember.Relationship.SPOUSE)
                            .dateOfBirth(LocalDate.now().minusYears(32))
                            .build(),
                    FamilyMember.builder()
                            .name("Child 1")
                            .relationship(FamilyMember.Relationship.CHILD)
                            .dateOfBirth(LocalDate.now().minusYears(8))
                            .build(),
                    FamilyMember.builder()
                            .name("Child 2")
                            .relationship(FamilyMember.Relationship.CHILD)
                            .dateOfBirth(LocalDate.now().minusYears(5))
                            .build()
            );

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(family);
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getHealthRecommendation().getTotalRecommendedCover())
                    .isGreaterThanOrEqualTo(1000000); // At least 10L base for family (Super Top-Up is supplementary)
        }

        @Test
        @DisplayName("should recommend separate policy for senior parents")
        void shouldRecommendSeparatePolicyForSeniorParents() {
            List<FamilyMember> family = Arrays.asList(
                    FamilyMember.builder()
                            .name("Self")
                            .relationship(FamilyMember.Relationship.SELF)
                            .dateOfBirth(LocalDate.now().minusYears(40))
                            .build(),
                    FamilyMember.builder()
                            .name("Father")
                            .relationship(FamilyMember.Relationship.PARENT)
                            .dateOfBirth(LocalDate.now().minusYears(68))
                            .build()
            );

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(family);
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            // Should have separate breakdown for senior parent
            assertThat(result.getHealthRecommendation().getMemberBreakdown())
                    .anyMatch(b -> b.getMemberType().equals("PARENT"));
        }

        @Test
        @DisplayName("should calculate coverage gap correctly")
        void shouldCalculateCoverageGapCorrectly() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            Insurance existingPolicy = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(Insurance.HealthInsuranceType.PERSONAL)
                    .sumAssured(300000.0) // Only 3L cover
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(existingPolicy));
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getHealthRecommendation().getExistingCover()).isEqualTo(300000.0);
            assertThat(result.getHealthRecommendation().getGap()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Term Insurance Recommendations")
    class TermInsuranceRecommendations {

        @Test
        @DisplayName("should calculate term cover based on income multiplier for young person")
        void shouldCalculateTermCoverBasedOnIncomeMultiplierForYoungPerson() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(30))
                    .build();

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            Settings settings = Settings.builder()
                    .currentAge(30)
                    .retirementAge(60)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            // Under 35: 15x income multiplier
            // 12L annual income × 15 = 1.8 Cr (plus liabilities and expenses)
            assertThat(result.getTermRecommendation().getBreakdown().getIncomeMultiplier()).isEqualTo(15);
            assertThat(result.getTermRecommendation().getBreakdown().getIncomeReplacement())
                    .isEqualTo(1200000.0 * 15); // 15x annual income
        }

        @Test
        @DisplayName("should include liability coverage in term recommendation")
        void shouldIncludeLiabilityCoverageInTermRecommendation() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            Loan homeLoan = Loan.builder()
                    .name("Home Loan")
                    .outstandingAmount(5000000.0)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(homeLoan));
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getTermRecommendation().getBreakdown().getTotalLiabilities())
                    .isEqualTo(5000000.0);
            assertThat(result.getTermRecommendation().getBreakdown().getLiabilityCoverage())
                    .isEqualTo(5000000.0);
        }

        @Test
        @DisplayName("should include children's future expenses in term recommendation")
        void shouldIncludeChildrensFutureExpensesInTermRecommendation() {
            List<FamilyMember> family = Arrays.asList(
                    FamilyMember.builder()
                            .name("Self")
                            .relationship(FamilyMember.Relationship.SELF)
                            .dateOfBirth(LocalDate.now().minusYears(35))
                            .build(),
                    FamilyMember.builder()
                            .name("Child")
                            .relationship(FamilyMember.Relationship.CHILD)
                            .dateOfBirth(LocalDate.now().minusYears(5))
                            .build()
            );

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(family);
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getTermRecommendation().getBreakdown().getChildCount()).isEqualTo(1);
            assertThat(result.getTermRecommendation().getBreakdown().getChildrenFutureCost())
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("should adjust cover based on earning spouse")
        void shouldAdjustCoverBasedOnEarningSpouse() {
            List<FamilyMember> family = Arrays.asList(
                    FamilyMember.builder()
                            .name("Self")
                            .relationship(FamilyMember.Relationship.SELF)
                            .dateOfBirth(LocalDate.now().minusYears(35))
                            .isEarning(true)
                            .monthlyIncome(100000.0)
                            .build(),
                    FamilyMember.builder()
                            .name("Spouse")
                            .relationship(FamilyMember.Relationship.SPOUSE)
                            .dateOfBirth(LocalDate.now().minusYears(32))
                            .isEarning(true)
                            .monthlyIncome(80000.0)
                            .build()
            );

            Income selfIncome = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(family);
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(selfIncome));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            // Should have spouse adjustment factor < 1.0
            assertThat(result.getTermRecommendation().getBreakdown().getSpouseAdjustmentFactor())
                    .isLessThan(1.0);
            assertThat(result.getTermRecommendation().getBreakdown().getSpouseAdjustmentReason())
                    .contains("Spouse earning");
        }
    }

    @Nested
    @DisplayName("Summary and Urgency")
    class SummaryAndUrgency {

        @Test
        @DisplayName("should show critical status when no insurance exists")
        void shouldShowCriticalStatusWhenNoInsuranceExists() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getSummary().getOverallScore()).isLessThan(50);
            assertThat(result.getSummary().getUrgentActions()).isNotEmpty();
        }

        @Test
        @DisplayName("should show adequate status when well covered")
        void shouldShowAdequateStatusWhenWellCovered() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            List<Insurance> policies = Arrays.asList(
                    Insurance.builder()
                            .type(Insurance.InsuranceType.HEALTH)
                            .healthType(Insurance.HealthInsuranceType.FAMILY_FLOATER)
                            .sumAssured(2000000.0) // 20L health cover
                            .build(),
                    Insurance.builder()
                            .type(Insurance.InsuranceType.TERM_LIFE)
                            .sumAssured(20000000.0) // 2Cr term cover
                            .build()
            );

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(policies);
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getSummary().getOverallScore()).isGreaterThan(50);
        }

        @Test
        @DisplayName("should calculate premium as percentage of income")
        void shouldCalculatePremiumAsPercentageOfIncome() {
            FamilyMember self = FamilyMember.builder()
                    .name("Self")
                    .relationship(FamilyMember.Relationship.SELF)
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .build();

            Income income = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .build();

            when(familyMemberRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(self));
            when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.singletonList(income));
            when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceRecommendation result = service.generateRecommendations(USER_ID);

            assertThat(result.getSummary().getPremiumAsPercentOfIncome()).isGreaterThan(0);
            assertThat(result.getSummary().getTotalEstimatedPremium()).isGreaterThan(0);
        }
    }
}
