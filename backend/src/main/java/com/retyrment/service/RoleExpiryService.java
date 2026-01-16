package com.retyrment.service;

import com.retyrment.model.User;
import com.retyrment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to handle automatic role expiry and reversion.
 * Runs periodically to check for expired temporary roles and revert them to original roles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleExpiryService {

    private final UserRepository userRepository;

    /**
     * Check for expired roles every hour and revert them to original roles.
     * Runs at minute 0 of every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void checkAndRevertExpiredRoles() {
        log.info("Checking for expired user roles...");
        
        LocalDateTime now = LocalDateTime.now();
        List<User> usersWithExpiredRoles = findUsersWithExpiredRoles(now);
        
        int revertedCount = 0;
        for (User user : usersWithExpiredRoles) {
            try {
                revertUserRole(user, now);
                revertedCount++;
            } catch (Exception e) {
                log.error("Failed to revert role for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
        
        if (revertedCount > 0) {
            log.info("Reverted {} expired user roles", revertedCount);
        } else {
            log.debug("No expired roles found");
        }
    }

    /**
     * Find all users whose temporary role has expired.
     *
     * @param now Current date and time for comparison
     * @return List of users with expired roles
     */
    private List<User> findUsersWithExpiredRoles(final LocalDateTime now) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoleExpiryDate() != null)
                .filter(user -> user.getRoleExpiryDate().isBefore(now))
                .filter(user -> user.getOriginalRole() != null)
                .toList();
    }

    /**
     * Revert a user's role to their original role after expiry.
     *
     * @param user User whose role should be reverted
     * @param now Current date and time for timestamp
     */
    private void revertUserRole(final User user, final LocalDateTime now) {
        User.UserRole expiredRole = user.getRole();
        User.UserRole originalRole = user.getOriginalRole();
        
        log.info("Reverting user {} from {} to {} (role expired)", 
                user.getEmail(), expiredRole, originalRole);
        
        user.setRole(originalRole);
        user.setOriginalRole(null);
        user.setRoleExpiryDate(null);
        user.setRoleChangeReason("Auto-reverted: Previous role expired");
        user.setRoleChangedAt(now);
        user.setRoleChangedBy("SYSTEM");
        
        userRepository.save(user);
    }

    /**
     * Manually trigger role expiry check (for admin use).
     * Returns the number of roles reverted.
     */
    public int forceCheckExpiredRoles() {
        log.info("Force checking expired roles...");
        
        LocalDateTime now = LocalDateTime.now();
        List<User> usersWithExpiredRoles = findUsersWithExpiredRoles(now);
        
        int revertedCount = 0;
        for (User user : usersWithExpiredRoles) {
            try {
                revertUserRole(user, now);
                revertedCount++;
            } catch (Exception e) {
                log.error("Failed to revert role for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
        
        return revertedCount;
    }

    /**
     * Get count of users with expiring roles in the next N days.
     *
     * @param withinDays Number of days to look ahead
     * @return Count of users with roles expiring within the specified days
     */
    public long countExpiringRoles(final int withinDays) {
        LocalDateTime deadline = LocalDateTime.now().plusDays(withinDays);
        return userRepository.findAll().stream()
                .filter(user -> user.getRoleExpiryDate() != null)
                .filter(user -> user.getRoleExpiryDate().isBefore(deadline))
                .filter(user -> user.getRoleExpiryDate().isAfter(LocalDateTime.now()))
                .count();
    }
}
