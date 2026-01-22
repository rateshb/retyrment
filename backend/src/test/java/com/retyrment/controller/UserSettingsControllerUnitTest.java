package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.model.UserSettings;
import com.retyrment.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingsController Unit Tests")
class UserSettingsControllerUnitTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserSettingsController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        testUser = User.builder().id("user-1").email("user1@example.com").build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    @DisplayName("getUserSettings returns defaults when none exist")
    void getUserSettings_returnsDefaults() {
        when(userSettingsRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getUserSettings();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("currentAge")).isEqualTo(35);
        assertThat(body.get("retirementAge")).isEqualTo(60);
        assertThat(body.get("lifeExpectancy")).isEqualTo(85);
        assertThat(body.get("inflationRate")).isEqualTo(6.0);
        assertThat(body.get("epfReturn")).isEqualTo(10.0);
        assertThat(body.get("ppfReturn")).isEqualTo(7.1);
        assertThat(body.get("mfEquityReturn")).isEqualTo(12.0);
        assertThat(body.get("mfDebtReturn")).isEqualTo(7.0);
        assertThat(body.get("fdReturn")).isEqualTo(6.5);
        assertThat(body.get("emergencyFundMonths")).isEqualTo(6);
        assertThat(body.get("sipStepup")).isEqualTo(10.0);
    }

    @Test
    @DisplayName("getUserSettings returns saved values with null fallbacks")
    void getUserSettings_returnsSavedValues() {
        UserSettings settings = UserSettings.builder()
                .userId("user-1")
                .currentAge(40)
                .retirementAge(65)
                .lifeExpectancy(90)
                .inflationRate(5.5)
                .epfReturn(null)
                .ppfReturn(7.5)
                .mfEquityReturn(13.0)
                .mfDebtReturn(6.0)
                .fdReturn(6.0)
                .emergencyFundMonths(9)
                .sipStepup(12.0)
                .build();
        when(userSettingsRepository.findByUserId("user-1")).thenReturn(Optional.of(settings));

        ResponseEntity<Map<String, Object>> response = controller.getUserSettings();
        Map<String, Object> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("currentAge")).isEqualTo(40);
        assertThat(body.get("retirementAge")).isEqualTo(65);
        assertThat(body.get("lifeExpectancy")).isEqualTo(90);
        assertThat(body.get("inflationRate")).isEqualTo(5.5);
        assertThat(body.get("epfReturn")).isEqualTo(10.0);
        assertThat(body.get("ppfReturn")).isEqualTo(7.5);
        assertThat(body.get("mfEquityReturn")).isEqualTo(13.0);
        assertThat(body.get("emergencyFundMonths")).isEqualTo(9);
        assertThat(body.get("sipStepup")).isEqualTo(12.0);
    }

    @Test
    @DisplayName("updateUserSettings creates new settings when none exist")
    void updateUserSettings_createsNew() {
        when(userSettingsRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("currentAge", 41);
        payload.put("retirementAge", 62);
        payload.put("lifeExpectancy", 86);
        payload.put("inflationRate", 6.2);
        payload.put("epfReturn", 8.15);
        payload.put("ppfReturn", 7.1);
        payload.put("mfEquityReturn", 12.5);
        payload.put("mfDebtReturn", 7.2);
        payload.put("fdReturn", 6.4);
        payload.put("emergencyFundMonths", 8);
        payload.put("sipStepup", 11.0);

        ResponseEntity<Map<String, Object>> response = controller.updateUserSettings(payload);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(captor.capture());
        UserSettings saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getCurrentAge()).isEqualTo(41);
        assertThat(saved.getRetirementAge()).isEqualTo(62);
        assertThat(saved.getLifeExpectancy()).isEqualTo(86);
        assertThat(saved.getInflationRate()).isEqualTo(6.2);
        assertThat(saved.getEpfReturn()).isEqualTo(8.15);
        assertThat(saved.getPpfReturn()).isEqualTo(7.1);
        assertThat(saved.getMfEquityReturn()).isEqualTo(12.5);
        assertThat(saved.getMfDebtReturn()).isEqualTo(7.2);
        assertThat(saved.getFdReturn()).isEqualTo(6.4);
        assertThat(saved.getEmergencyFundMonths()).isEqualTo(8);
        assertThat(saved.getSipStepup()).isEqualTo(11.0);
    }

    @Test
    @DisplayName("updateUserSettings updates existing settings")
    void updateUserSettings_updatesExisting() {
        UserSettings existing = UserSettings.builder().userId("user-1").currentAge(35).build();
        when(userSettingsRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("currentAge", 38);
        payload.put("inflationRate", 5.8);
        payload.put("sipStepup", 9.0);

        ResponseEntity<Map<String, Object>> response = controller.updateUserSettings(payload);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(captor.capture());
        UserSettings saved = captor.getValue();
        assertThat(saved.getCurrentAge()).isEqualTo(38);
        assertThat(saved.getInflationRate()).isEqualTo(5.8);
        assertThat(saved.getSipStepup()).isEqualTo(9.0);
    }
}
