package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.model.UserPreference;
import com.retyrment.repository.UserPreferenceRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserPreferenceController Unit Tests")
class UserPreferenceControllerUnitTest {

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserPreferenceController preferenceController;

    private User testUser;
    private UserPreference testPreferences;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();

        testPreferences = UserPreference.builder()
                .id("pref-1")
                .userId("user-1")
                .currency("INR")
                .currencySymbol("₹")
                .country("IN")
                .locale("en-IN")
                .dateFormat("DD/MM/YYYY")
                .numberFormat("indian")
                .showAmountInWords(true)
                .theme("light")
                .financialYearStartMonth(4)
                .defaultInflationRate(6.0)
                .defaultEquityReturn(12.0)
                .defaultDebtReturn(7.0)
                .defaultRetirementAge(60)
                .emailNotifications(true)
                .paymentReminders(true)
                .reminderDaysBefore(3)
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserId {
        @Test
        @DisplayName("should return user ID when authenticated")
        void shouldReturnUserIdWhenAuthenticated() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));

            ResponseEntity<UserPreference> response = preferenceController.getPreferences();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should return default when authentication is null")
        void shouldReturnDefaultWhenAuthIsNull() {
            when(securityContext.getAuthentication()).thenReturn(null);
            when(preferenceRepository.findByUserId("default")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> {
                UserPreference pref = inv.getArgument(0);
                pref.setId("new-pref-id");
                return pref;
            });

            ResponseEntity<UserPreference> response = preferenceController.getPreferences();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo("default");
        }

        @Test
        @DisplayName("should return default when principal is not User")
        void shouldReturnDefaultWhenPrincipalNotUser() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            when(preferenceRepository.findByUserId("default")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> {
                UserPreference pref = inv.getArgument(0);
                pref.setId("new-pref-id");
                return pref;
            });

            ResponseEntity<UserPreference> response = preferenceController.getPreferences();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {
        @Test
        @DisplayName("should return existing preferences when found")
        void shouldReturnExistingPreferences() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));

            ResponseEntity<UserPreference> response = preferenceController.getPreferences();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(testPreferences);
            verify(preferenceRepository).findByUserId("user-1");
            verify(preferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create default preferences when not found")
        void shouldCreateDefaultPreferencesWhenNotFound() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> {
                UserPreference pref = inv.getArgument(0);
                pref.setId("new-pref-id");
                return pref;
            });

            ResponseEntity<UserPreference> response = preferenceController.getPreferences();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo("user-1");
            assertThat(response.getBody().getCurrency()).isEqualTo("INR");
            assertThat(response.getBody().getCountry()).isEqualTo("IN");
            verify(preferenceRepository).save(any(UserPreference.class));
        }
    }

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {
        @Test
        @DisplayName("should update existing preferences")
        void shouldUpdateExistingPreferences() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .currency("USD")
                    .dateFormat("MM/DD/YYYY")
                    .theme("dark")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(any(UserPreference.class));
        }

        @Test
        @DisplayName("should create default preferences when updating non-existent preferences")
        void shouldCreateDefaultWhenUpdatingNonExistent() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> {
                UserPreference pref = inv.getArgument(0);
                pref.setId("new-pref-id");
                return pref;
            });

            UserPreference updates = UserPreference.builder()
                    .currency("USD")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            verify(preferenceRepository, times(2)).save(any(UserPreference.class)); // Once for create, once for update
        }

        @Test
        @DisplayName("should update currency and auto-set symbol and locale")
        void shouldUpdateCurrencyAndAutoSetSymbol() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .currency("USD")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getCurrency().equals("USD") && 
                pref.getCurrencySymbol() != null && 
                pref.getLocale() != null));
        }

        @Test
        @DisplayName("should handle invalid currency value")
        void shouldHandleInvalidCurrency() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .currency("INVALID")
                    .currencySymbol("$")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(any(UserPreference.class));
        }

        @Test
        @DisplayName("should update country and auto-set financial year start month")
        void shouldUpdateCountryAndAutoSetFyStartMonth() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .country("US")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getCountry().equals("US") && 
                pref.getFinancialYearStartMonth() != null));
        }

        @Test
        @DisplayName("should handle invalid country value")
        void shouldHandleInvalidCountry() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .country("INVALID")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(any(UserPreference.class));
        }

        @Test
        @DisplayName("should update dateFormat when provided")
        void shouldUpdateDateFormat() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .dateFormat("YYYY-MM-DD")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getDateFormat().equals("YYYY-MM-DD")));
        }

        @Test
        @DisplayName("should update numberFormat when provided")
        void shouldUpdateNumberFormat() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .numberFormat("western")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getNumberFormat().equals("western")));
        }

        @Test
        @DisplayName("should update showAmountInWords when provided")
        void shouldUpdateShowAmountInWords() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .showAmountInWords(false)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                !pref.getShowAmountInWords()));
        }

        @Test
        @DisplayName("should update theme when provided")
        void shouldUpdateTheme() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .theme("dark")
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getTheme().equals("dark")));
        }

        @Test
        @DisplayName("should update financialYearStartMonth when provided")
        void shouldUpdateFinancialYearStartMonth() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .financialYearStartMonth(1)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getFinancialYearStartMonth() == 1));
        }

        @Test
        @DisplayName("should update defaultInflationRate when provided")
        void shouldUpdateDefaultInflationRate() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .defaultInflationRate(7.0)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getDefaultInflationRate() == 7.0));
        }

        @Test
        @DisplayName("should update defaultEquityReturn when provided")
        void shouldUpdateDefaultEquityReturn() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .defaultEquityReturn(13.0)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getDefaultEquityReturn() == 13.0));
        }

        @Test
        @DisplayName("should update defaultDebtReturn when provided")
        void shouldUpdateDefaultDebtReturn() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .defaultDebtReturn(8.0)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getDefaultDebtReturn() == 8.0));
        }

        @Test
        @DisplayName("should update defaultRetirementAge when provided")
        void shouldUpdateDefaultRetirementAge() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .defaultRetirementAge(65)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getDefaultRetirementAge() == 65));
        }

        @Test
        @DisplayName("should update emailNotifications when provided")
        void shouldUpdateEmailNotifications() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .emailNotifications(false)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                !pref.getEmailNotifications()));
        }

        @Test
        @DisplayName("should update paymentReminders when provided")
        void shouldUpdatePaymentReminders() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .paymentReminders(false)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                !pref.getPaymentReminders()));
        }

        @Test
        @DisplayName("should update reminderDaysBefore when provided")
        void shouldUpdateReminderDaysBefore() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .reminderDaysBefore(7)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(argThat(pref -> 
                pref.getReminderDaysBefore() == 7));
        }

        @Test
        @DisplayName("should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(testPreferences));
            when(preferenceRepository.save(any(UserPreference.class))).thenReturn(testPreferences);

            UserPreference updates = UserPreference.builder()
                    .currency(null)
                    .country(null)
                    .dateFormat(null)
                    .build();

            ResponseEntity<UserPreference> response = preferenceController.updatePreferences(updates);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(preferenceRepository).save(any(UserPreference.class));
        }
    }

    @Nested
    @DisplayName("getOptions")
    class GetOptions {
        @Test
        @DisplayName("should return all available options")
        void shouldReturnAllOptions() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            Map<String, Object> options = response.getBody();
            assertThat(options).isNotNull();
            assertThat(options).containsKeys("currencies", "countries", "dateFormats", "numberFormats", "themes");
        }

        @Test
        @DisplayName("should include all currencies")
        void shouldIncludeAllCurrencies() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, String>> currencies = (java.util.List<Map<String, String>>) response.getBody().get("currencies");
            assertThat(currencies).isNotEmpty();
            assertThat(currencies.get(0)).containsKeys("code", "symbol", "name");
        }

        @Test
        @DisplayName("should include all countries")
        void shouldIncludeAllCountries() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> countries = (java.util.List<Map<String, Object>>) response.getBody().get("countries");
            assertThat(countries).isNotEmpty();
            assertThat(countries.get(0)).containsKeys("code", "name", "defaultCurrency", "fyStartMonth");
        }

        @Test
        @DisplayName("should include date formats")
        void shouldIncludeDateFormats() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, String>> dateFormats = (java.util.List<Map<String, String>>) response.getBody().get("dateFormats");
            assertThat(dateFormats).hasSize(3);
            assertThat(dateFormats.get(0)).containsKey("value");
            assertThat(dateFormats.get(0)).containsKey("label");
        }

        @Test
        @DisplayName("should include number formats")
        void shouldIncludeNumberFormats() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, String>> numberFormats = (java.util.List<Map<String, String>>) response.getBody().get("numberFormats");
            assertThat(numberFormats).hasSize(2);
            assertThat(numberFormats.get(0)).containsKey("value");
            assertThat(numberFormats.get(0)).containsKey("label");
        }

        @Test
        @DisplayName("should include themes")
        void shouldIncludeThemes() {
            ResponseEntity<Map<String, Object>> response = preferenceController.getOptions();

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, String>> themes = (java.util.List<Map<String, String>>) response.getBody().get("themes");
            assertThat(themes).hasSize(3);
            assertThat(themes.get(0)).containsKey("value");
            assertThat(themes.get(0)).containsKey("label");
        }
    }
}
