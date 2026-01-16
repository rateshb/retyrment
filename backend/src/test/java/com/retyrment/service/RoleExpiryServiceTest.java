package com.retyrment.service;

import com.retyrment.model.User;
import com.retyrment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleExpiryService Tests")
class RoleExpiryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleExpiryService roleExpiryService;

    private User userWithExpiredRole;
    private User userWithActiveRole;
    private User userWithNoExpiry;

    @BeforeEach
    void setUp() {
        // User with expired role
        userWithExpiredRole = User.builder()
                .id("user1")
                .email("expired@example.com")
                .role(User.UserRole.PRO)
                .originalRole(User.UserRole.FREE)
                .roleExpiryDate(LocalDateTime.now().minusDays(1))
                .build();

        // User with active (not expired) role
        userWithActiveRole = User.builder()
                .id("user2")
                .email("active@example.com")
                .role(User.UserRole.PRO)
                .originalRole(User.UserRole.FREE)
                .roleExpiryDate(LocalDateTime.now().plusDays(5))
                .build();

        // User with no expiry (permanent role)
        userWithNoExpiry = User.builder()
                .id("user3")
                .email("permanent@example.com")
                .role(User.UserRole.PRO)
                .originalRole(null)
                .roleExpiryDate(null)
                .build();
    }

    @Nested
    @DisplayName("checkAndRevertExpiredRoles")
    class CheckAndRevertExpiredRoles {
        @Test
        @DisplayName("should revert expired roles")
        void shouldRevertExpiredRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithExpiredRole, userWithActiveRole));
            when(userRepository.save(any(User.class))).thenReturn(userWithExpiredRole);

            roleExpiryService.checkAndRevertExpiredRoles();

            verify(userRepository, times(1)).save(any(User.class));
            verify(userRepository).save(argThat(user -> 
                user.getRole() == User.UserRole.FREE && 
                user.getOriginalRole() == null &&
                user.getRoleExpiryDate() == null
            ));
        }

        @Test
        @DisplayName("should not revert non-expired roles")
        void shouldNotRevertNonExpiredRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithActiveRole, userWithNoExpiry));

            roleExpiryService.checkAndRevertExpiredRoles();

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should handle empty user list")
        void shouldHandleEmptyUserList() {
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            roleExpiryService.checkAndRevertExpiredRoles();

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should handle users without original role")
        void shouldHandleUsersWithoutOriginalRole() {
            User userWithoutOriginal = User.builder()
                    .id("user4")
                    .email("nooriginal@example.com")
                    .role(User.UserRole.PRO)
                    .originalRole(null)
                    .roleExpiryDate(LocalDateTime.now().minusDays(1))
                    .build();

            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithoutOriginal));

            roleExpiryService.checkAndRevertExpiredRoles();

            // Should not revert if no original role
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should handle exception during revert")
        void shouldHandleExceptionDuringRevert() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithExpiredRole));
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // Should not throw exception, just log error
            roleExpiryService.checkAndRevertExpiredRoles();

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("forceCheckExpiredRoles")
    class ForceCheckExpiredRoles {
        @Test
        @DisplayName("should return count of reverted roles")
        void shouldReturnCountOfRevertedRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithExpiredRole, userWithActiveRole));
            when(userRepository.save(any(User.class))).thenReturn(userWithExpiredRole);

            int count = roleExpiryService.forceCheckExpiredRoles();

            assertThat(count).isEqualTo(1);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should return zero when no expired roles")
        void shouldReturnZeroWhenNoExpiredRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithActiveRole, userWithNoExpiry));

            int count = roleExpiryService.forceCheckExpiredRoles();

            assertThat(count).isEqualTo(0);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("countExpiringRoles")
    class CountExpiringRoles {
        @Test
        @DisplayName("should count roles expiring within days")
        void shouldCountRolesExpiringWithinDays() {
            User expiringSoon = User.builder()
                    .id("user5")
                    .email("expiring@example.com")
                    .role(User.UserRole.PRO)
                    .roleExpiryDate(LocalDateTime.now().plusDays(3))
                    .build();

            when(userRepository.findAll()).thenReturn(Arrays.asList(
                    expiringSoon,
                    userWithActiveRole,
                    userWithNoExpiry
            ));

            long count = roleExpiryService.countExpiringRoles(7);

            assertThat(count).isEqualTo(2); // expiringSoon and userWithActiveRole
        }

        @Test
        @DisplayName("should not count already expired roles")
        void shouldNotCountAlreadyExpiredRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(
                    userWithExpiredRole,
                    userWithActiveRole
            ));

            long count = roleExpiryService.countExpiringRoles(7);

            assertThat(count).isEqualTo(1); // Only userWithActiveRole
        }

        @Test
        @DisplayName("should return zero when no expiring roles")
        void shouldReturnZeroWhenNoExpiringRoles() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithNoExpiry));

            long count = roleExpiryService.countExpiringRoles(7);

            assertThat(count).isEqualTo(0);
        }
    }
}
