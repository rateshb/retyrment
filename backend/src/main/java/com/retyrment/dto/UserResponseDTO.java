package com.retyrment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.retyrment.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized user response DTO to eliminate duplicate response structures.
 * Used by AuthController and AdminController to return consistent user data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {
    
    private String id;
    private String email;
    private String name;
    private String picture;
    private String role;
    private String effectiveRole;
    private Boolean isPro;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    // Nested objects
    private TrialInfo trial;
    private SubscriptionInfo subscription;
    private RoleInfo roleInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrialInfo {
        private Boolean active;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Long daysRemaining;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionInfo {
        private Boolean active;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Long daysRemaining;
        private Boolean expired;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleInfo {
        private Boolean temporary;
        private LocalDateTime expiryDate;
        private Long daysRemaining;
        private Boolean expired;
        private String originalRole;
        private String reason;
        private LocalDateTime changedAt;
        private String changedBy;
    }
    
    /**
     * Convert User entity to UserResponseDTO
     */
    public static UserResponseDTO fromUser(User user, boolean includeAdminFields) {
        UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .picture(user.getPicture())
                .role(user.getRole().name())
                .effectiveRole(user.getEffectiveRole().name())
                .isPro(user.isPro())
                .isAdmin(user.isAdmin());
        
        if (includeAdminFields) {
            builder.createdAt(user.getCreatedAt())
                   .lastLoginAt(user.getLastLoginAt());
        }
        
        // Trial information
        if (user.isInTrial()) {
            builder.trial(TrialInfo.builder()
                    .active(true)
                    .startDate(user.getTrialStartDate())
                    .endDate(user.getTrialEndDate())
                    .daysRemaining(user.getTrialDaysRemaining())
                    .build());
        } else if (includeAdminFields && user.getTrialEndDate() != null) {
            builder.trial(TrialInfo.builder()
                    .active(false)
                    .startDate(user.getTrialStartDate())
                    .endDate(user.getTrialEndDate())
                    .daysRemaining(user.getTrialDaysRemaining())
                    .build());
        }
        
        // PRO Subscription information
        if (user.getRole() == User.UserRole.PRO) {
            builder.subscription(SubscriptionInfo.builder()
                    .active(user.isSubscriptionActive())
                    .startDate(user.getSubscriptionStartDate())
                    .endDate(user.getSubscriptionEndDate())
                    .daysRemaining(user.getSubscriptionDaysRemaining())
                    .expired(!user.isSubscriptionActive())
                    .build());
        }
        
        // Role expiry information
        if (user.getRoleExpiryDate() != null) {
            RoleInfo.RoleInfoBuilder roleInfoBuilder = RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(user.getRoleExpiryDate())
                    .daysRemaining(user.getRoleDaysRemaining())
                    .expired(user.isRoleExpired())
                    .originalRole(user.getOriginalRole() != null ? user.getOriginalRole().name() : null)
                    .reason(user.getRoleChangeReason());
            
            if (includeAdminFields) {
                roleInfoBuilder.changedAt(user.getRoleChangedAt())
                              .changedBy(user.getRoleChangedBy());
            }
            
            builder.roleInfo(roleInfoBuilder.build());
        }
        
        return builder.build();
    }
    
    /**
     * Convert to Map for backward compatibility (if needed)
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("name", name);
        map.put("picture", picture);
        map.put("role", role);
        map.put("effectiveRole", effectiveRole);
        map.put("isPro", isPro);
        map.put("isAdmin", isAdmin);
        
        if (createdAt != null) {
            map.put("createdAt", createdAt);
        }
        if (lastLoginAt != null) {
            map.put("lastLoginAt", lastLoginAt);
        }
        if (trial != null) {
            Map<String, Object> trialMap = new HashMap<>();
            trialMap.put("active", trial.getActive());
            if (trial.getStartDate() != null) trialMap.put("startDate", trial.getStartDate());
            if (trial.getEndDate() != null) trialMap.put("endDate", trial.getEndDate());
            if (trial.getDaysRemaining() != null) trialMap.put("daysRemaining", trial.getDaysRemaining());
            map.put("trial", trialMap);
        }
        if (subscription != null) {
            Map<String, Object> subMap = new HashMap<>();
            subMap.put("active", subscription.getActive());
            if (subscription.getStartDate() != null) subMap.put("startDate", subscription.getStartDate());
            if (subscription.getEndDate() != null) subMap.put("endDate", subscription.getEndDate());
            if (subscription.getDaysRemaining() != null) subMap.put("daysRemaining", subscription.getDaysRemaining());
            if (subscription.getExpired() != null) subMap.put("expired", subscription.getExpired());
            map.put("subscription", subMap);
        }
        if (roleInfo != null) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("temporary", roleInfo.getTemporary());
            if (roleInfo.getExpiryDate() != null) roleMap.put("expiryDate", roleInfo.getExpiryDate());
            if (roleInfo.getDaysRemaining() != null) roleMap.put("daysRemaining", roleInfo.getDaysRemaining());
            if (roleInfo.getExpired() != null) roleMap.put("expired", roleInfo.getExpired());
            if (roleInfo.getOriginalRole() != null) roleMap.put("originalRole", roleInfo.getOriginalRole());
            if (roleInfo.getReason() != null) roleMap.put("reason", roleInfo.getReason());
            if (roleInfo.getChangedAt() != null) roleMap.put("changedAt", roleInfo.getChangedAt());
            if (roleInfo.getChangedBy() != null) roleMap.put("changedBy", roleInfo.getChangedBy());
            map.put("roleInfo", roleMap);
        }
        
        return map;
    }
}
