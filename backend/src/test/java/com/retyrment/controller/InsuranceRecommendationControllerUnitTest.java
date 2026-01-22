package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.InsuranceRecommendationService;
import com.retyrment.service.InsuranceRecommendationService.InsuranceRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceRecommendationController Unit Tests")
class InsuranceRecommendationControllerUnitTest {

    @Mock
    private InsuranceRecommendationService recommendationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InsuranceRecommendationController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        testUser = User.builder().id("user-1").email("user1@example.com").build();
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    @DisplayName("getRecommendations returns service result")
    void getRecommendations_returnsServiceResult() {
        InsuranceRecommendation rec = new InsuranceRecommendation();
        when(recommendationService.generateRecommendations("user-1")).thenReturn(rec);

        InsuranceRecommendation result = controller.getRecommendations();

        assertThat(result).isSameAs(rec);
    }

    @Test
    @DisplayName("getRecommendations throws when unauthenticated")
    void getRecommendations_throwsWhenUnauthenticated() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThatThrownBy(() -> controller.getRecommendations())
                .isInstanceOf(IllegalStateException.class);
    }
}
