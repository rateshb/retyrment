package com.retyrment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FamilyMember model.
 */
@DisplayName("FamilyMember")
class FamilyMemberTest {

    @Nested
    @DisplayName("Age Calculations")
    class AgeCalculations {

        @Test
        @DisplayName("should calculate current age from date of birth")
        void shouldCalculateCurrentAgeFromDateOfBirth() {
            FamilyMember member = FamilyMember.builder()
                    .name("John Doe")
                    .dateOfBirth(LocalDate.now().minusYears(35).minusDays(10))
                    .build();

            Integer age = member.getCurrentAge();

            assertThat(age).isEqualTo(35);
        }

        @Test
        @DisplayName("should return null when date of birth is not set")
        void shouldReturnNullWhenDateOfBirthNotSet() {
            FamilyMember member = FamilyMember.builder()
                    .name("John Doe")
                    .build();

            Integer age = member.getCurrentAge();

            assertThat(age).isNull();
        }

        @Test
        @DisplayName("should handle birthday not yet reached this year")
        void shouldHandleBirthdayNotYetReachedThisYear() {
            FamilyMember member = FamilyMember.builder()
                    .name("John Doe")
                    .dateOfBirth(LocalDate.now().minusYears(35).plusDays(10))
                    .build();

            Integer age = member.getCurrentAge();

            assertThat(age).isEqualTo(34); // Birthday not reached yet
        }
    }

    @Nested
    @DisplayName("Age Classification")
    class AgeClassification {

        @Test
        @DisplayName("should identify minor (under 18)")
        void shouldIdentifyMinor() {
            FamilyMember child = FamilyMember.builder()
                    .name("Child")
                    .dateOfBirth(LocalDate.now().minusYears(10))
                    .relationship(FamilyMember.Relationship.CHILD)
                    .build();

            assertThat(child.isMinor()).isTrue();
            assertThat(child.isSeniorCitizen()).isFalse();
            assertThat(child.isSuperSenior()).isFalse();
        }

        @Test
        @DisplayName("should identify adult (18-59)")
        void shouldIdentifyAdult() {
            FamilyMember adult = FamilyMember.builder()
                    .name("Adult")
                    .dateOfBirth(LocalDate.now().minusYears(35))
                    .relationship(FamilyMember.Relationship.SELF)
                    .build();

            assertThat(adult.isMinor()).isFalse();
            assertThat(adult.isSeniorCitizen()).isFalse();
            assertThat(adult.isSuperSenior()).isFalse();
        }

        @Test
        @DisplayName("should identify senior citizen (60-79)")
        void shouldIdentifySeniorCitizen() {
            FamilyMember senior = FamilyMember.builder()
                    .name("Parent")
                    .dateOfBirth(LocalDate.now().minusYears(65))
                    .relationship(FamilyMember.Relationship.PARENT)
                    .build();

            assertThat(senior.isMinor()).isFalse();
            assertThat(senior.isSeniorCitizen()).isTrue();
            assertThat(senior.isSuperSenior()).isFalse();
        }

        @Test
        @DisplayName("should identify super senior (80+)")
        void shouldIdentifySuperSenior() {
            FamilyMember superSenior = FamilyMember.builder()
                    .name("Grandparent")
                    .dateOfBirth(LocalDate.now().minusYears(85))
                    .relationship(FamilyMember.Relationship.PARENT)
                    .build();

            assertThat(superSenior.isMinor()).isFalse();
            assertThat(superSenior.isSeniorCitizen()).isTrue();
            assertThat(superSenior.isSuperSenior()).isTrue();
        }

        @Test
        @DisplayName("should return false for all classifications when age is null")
        void shouldReturnFalseForAllClassificationsWhenAgeIsNull() {
            FamilyMember member = FamilyMember.builder()
                    .name("Unknown")
                    .build();

            assertThat(member.isMinor()).isFalse();
            assertThat(member.isSeniorCitizen()).isFalse();
            assertThat(member.isSuperSenior()).isFalse();
        }
    }

    @Nested
    @DisplayName("Dependency Calculations")
    class DependencyCalculations {

        @Test
        @DisplayName("should calculate years of dependency for child")
        void shouldCalculateYearsOfDependencyForChild() {
            FamilyMember child = FamilyMember.builder()
                    .name("Child")
                    .dateOfBirth(LocalDate.now().minusYears(10))
                    .relationship(FamilyMember.Relationship.CHILD)
                    .dependencyEndAge(25)
                    .build();

            Integer yearsOfDependency = child.getYearsOfDependency();

            assertThat(yearsOfDependency).isEqualTo(15); // 25 - 10
        }

        @Test
        @DisplayName("should return zero when child has passed dependency end age")
        void shouldReturnZeroWhenChildHasPassedDependencyEndAge() {
            FamilyMember child = FamilyMember.builder()
                    .name("Adult Child")
                    .dateOfBirth(LocalDate.now().minusYears(28))
                    .relationship(FamilyMember.Relationship.CHILD)
                    .dependencyEndAge(25)
                    .build();

            Integer yearsOfDependency = child.getYearsOfDependency();

            assertThat(yearsOfDependency).isEqualTo(0);
        }

        @Test
        @DisplayName("should return null when dependency end age not set")
        void shouldReturnNullWhenDependencyEndAgeNotSet() {
            FamilyMember child = FamilyMember.builder()
                    .name("Child")
                    .dateOfBirth(LocalDate.now().minusYears(10))
                    .build();

            Integer yearsOfDependency = child.getYearsOfDependency();

            assertThat(yearsOfDependency).isNull();
        }
    }

    @Nested
    @DisplayName("Builder and Enums")
    class BuilderAndEnums {

        @Test
        @DisplayName("should build complete family member")
        void shouldBuildCompleteFamilyMember() {
            FamilyMember member = FamilyMember.builder()
                    .name("Jane Doe")
                    .relationship(FamilyMember.Relationship.SPOUSE)
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .gender(FamilyMember.Gender.FEMALE)
                    .isEarning(true)
                    .monthlyIncome(75000.0)
                    .isDependent(false)
                    .hasPreExistingConditions(false)
                    .isSmoker(false)
                    .existingHealthCover(500000.0)
                    .existingLifeCover(0.0)
                    .build();

            assertThat(member.getName()).isEqualTo("Jane Doe");
            assertThat(member.getRelationship()).isEqualTo(FamilyMember.Relationship.SPOUSE);
            assertThat(member.getGender()).isEqualTo(FamilyMember.Gender.FEMALE);
            assertThat(member.getIsEarning()).isTrue();
            assertThat(member.getMonthlyIncome()).isEqualTo(75000.0);
        }

        @Test
        @DisplayName("should have all relationship types")
        void shouldHaveAllRelationshipTypes() {
            assertThat(FamilyMember.Relationship.values()).containsExactlyInAnyOrder(
                    FamilyMember.Relationship.SELF,
                    FamilyMember.Relationship.SPOUSE,
                    FamilyMember.Relationship.CHILD,
                    FamilyMember.Relationship.PARENT,
                    FamilyMember.Relationship.PARENT_IN_LAW,
                    FamilyMember.Relationship.SIBLING,
                    FamilyMember.Relationship.OTHER
            );
        }

        @Test
        @DisplayName("should have all gender types")
        void shouldHaveAllGenderTypes() {
            assertThat(FamilyMember.Gender.values()).containsExactlyInAnyOrder(
                    FamilyMember.Gender.MALE,
                    FamilyMember.Gender.FEMALE,
                    FamilyMember.Gender.OTHER
            );
        }

        @Test
        @DisplayName("should have all education levels")
        void shouldHaveAllEducationLevels() {
            assertThat(FamilyMember.EducationLevel.values()).containsExactlyInAnyOrder(
                    FamilyMember.EducationLevel.PRE_SCHOOL,
                    FamilyMember.EducationLevel.PRIMARY,
                    FamilyMember.EducationLevel.MIDDLE,
                    FamilyMember.EducationLevel.SECONDARY,
                    FamilyMember.EducationLevel.HIGHER_SECONDARY,
                    FamilyMember.EducationLevel.UNDERGRADUATE,
                    FamilyMember.EducationLevel.POSTGRADUATE,
                    FamilyMember.EducationLevel.WORKING,
                    FamilyMember.EducationLevel.NOT_APPLICABLE
            );
        }
    }

    @Nested
    @DisplayName("Health Risk Factors")
    class HealthRiskFactors {

        @Test
        @DisplayName("should track pre-existing conditions")
        void shouldTrackPreExistingConditions() {
            FamilyMember member = FamilyMember.builder()
                    .name("Parent")
                    .hasPreExistingConditions(true)
                    .preExistingConditions("Diabetes, Hypertension")
                    .build();

            assertThat(member.getHasPreExistingConditions()).isTrue();
            assertThat(member.getPreExistingConditions()).isEqualTo("Diabetes, Hypertension");
        }

        @Test
        @DisplayName("should track smoking status")
        void shouldTrackSmokingStatus() {
            FamilyMember member = FamilyMember.builder()
                    .name("Self")
                    .isSmoker(true)
                    .build();

            assertThat(member.getIsSmoker()).isTrue();
        }
    }
}
