package com.retyrment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    
    public enum UserRole {
        FREE,   // Basic features - data entry, projections
        PRO,    // Premium features - recommendations, retirement planning, reports
        ADMIN   // All features + user management
    }
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String name;
    private String picture;
    private String providerId;  // Google user ID
    private String provider;    // "google"
    
    @Builder.Default
    private UserRole role = UserRole.FREE;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    // Trial period - all new users get 7 days of PRO
    private LocalDateTime trialStartDate;
    private LocalDateTime trialEndDate;
    
    // PRO subscription period - PRO users have time-limited subscriptions
    private LocalDateTime subscriptionStartDate;  // When PRO subscription started
    private LocalDateTime subscriptionEndDate;    // When PRO subscription ends (null = expired/inactive)
    
    // Time-limited role changes - for admin-granted temporary access
    private LocalDateTime roleExpiryDate;       // When current role expires (null = permanent)
    private UserRole originalRole;              // Role to revert to after expiry
    private String roleChangeReason;            // Reason for role change (e.g., "License purchase", "Trial extension")
    private LocalDateTime roleChangedAt;        // When the role was last changed
    private String roleChangedBy;               // Admin who changed the role
    
    // Helper methods for role checks
    public boolean isPro() {
        // Admin always has PRO access
        if (role == UserRole.ADMIN) {
            return true;
        }
        // Check if in trial period
        if (isInTrial()) {
            return true;
        }
        // Check if PRO role with active subscription
        if (role == UserRole.PRO) {
            return isSubscriptionActive();
        }
        return false;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isFree() {
        // User is FREE if not in trial and (not PRO or PRO subscription expired)
        if (isInTrial()) {
            return false;
        }
        if (role == UserRole.PRO && isSubscriptionActive()) {
            return false;
        }
        return role == UserRole.FREE || (role == UserRole.PRO && !isSubscriptionActive());
    }
    
    /**
     * Check if user is currently in 7-day PRO trial period
     */
    public boolean isInTrial() {
        if (trialEndDate == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(trialEndDate);
    }
    
    /**
     * Check if the current role has expired
     */
    public boolean isRoleExpired() {
        if (roleExpiryDate == null) {
            return false; // Permanent role
        }
        return LocalDateTime.now().isAfter(roleExpiryDate);
    }
    
    /**
     * Check if PRO subscription is currently active
     */
    public boolean isSubscriptionActive() {
        if (subscriptionEndDate == null) {
            return false; // No subscription end date means expired/inactive
        }
        LocalDateTime now = LocalDateTime.now();
        // Subscription is active if current time is between start and end dates
        boolean afterStart = subscriptionStartDate == null || now.isAfter(subscriptionStartDate) || now.isEqual(subscriptionStartDate);
        boolean beforeEnd = now.isBefore(subscriptionEndDate);
        return afterStart && beforeEnd;
    }
    
    /**
     * Get the effective role (considering trial, subscription expiry, and role expiry)
     */
    public UserRole getEffectiveRole() {
        // Admin role never expires
        if (role == UserRole.ADMIN) {
            return UserRole.ADMIN;
        }
        
        // If role has expired (temporary role change), return original role
        if (isRoleExpired() && originalRole != null) {
            return originalRole;
        }
        
        // If in trial and actual role is FREE, treat as PRO
        if (role == UserRole.FREE && isInTrial()) {
            return UserRole.PRO;
        }
        
        // If PRO role but subscription expired, treat as FREE
        if (role == UserRole.PRO && !isSubscriptionActive()) {
            return UserRole.FREE;
        }
        
        return role;
    }
    
    /**
     * Get days remaining in trial (0 if not in trial or expired)
     */
    public long getTrialDaysRemaining() {
        if (trialEndDate == null || !isInTrial()) {
            return 0;
        }
        long hours = java.time.Duration.between(LocalDateTime.now(), trialEndDate).toHours();
        return Math.max(0, (hours + 23) / 24); // Round up
    }
    
    /**
     * Get days remaining for current role (null if permanent)
     */
    public Long getRoleDaysRemaining() {
        if (roleExpiryDate == null) {
            return null; // Permanent
        }
        if (isRoleExpired()) {
            return 0L;
        }
        long hours = java.time.Duration.between(LocalDateTime.now(), roleExpiryDate).toHours();
        return Math.max(0, (hours + 23) / 24); // Round up
    }
    
    /**
     * Get days remaining in PRO subscription (0 if expired or not PRO)
     */
    public long getSubscriptionDaysRemaining() {
        if (subscriptionEndDate == null || !isSubscriptionActive()) {
            return 0;
        }
        long hours = java.time.Duration.between(LocalDateTime.now(), subscriptionEndDate).toHours();
        return Math.max(0, (hours + 23) / 24); // Round up
    }
}
