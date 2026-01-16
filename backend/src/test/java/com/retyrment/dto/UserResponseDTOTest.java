package com.retyrment.dto;

import com.retyrment.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserResponseDTO Tests")
class UserResponseDTOTest {

    @Nested
    @DisplayName("fromUser - Basic User")
    class FromUserBasic {

        @Test
        @DisplayName("should create DTO from basic FREE user")
        void shouldCreateFromBasicFreeUser() {
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .picture("https://example.com/pic.jpg")
                    .role(User.UserRole.FREE)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getId()).isEqualTo("user1");
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getName()).isEqualTo("Test User");
            assertThat(dto.getRole()).isEqualTo("FREE");
            assertThat(dto.getEffectiveRole()).isEqualTo("FREE");
            assertThat(dto.getIsPro()).isFalse();
            assertThat(dto.getIsAdmin()).isFalse();
            assertThat(dto.getTrial()).isNull();
            assertThat(dto.getSubscription()).isNull();
            assertThat(dto.getRoleInfo()).isNull();
        }

        @Test
        @DisplayName("should include admin fields when includeAdminFields is true")
        void shouldIncludeAdminFields() {
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .createdAt(now)
                    .lastLoginAt(now.plusDays(1))
                    .role(User.UserRole.FREE)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, true);

            assertThat(dto.getCreatedAt()).isEqualTo(now);
            assertThat(dto.getLastLoginAt()).isEqualTo(now.plusDays(1));
        }

        @Test
        @DisplayName("should not include admin fields when includeAdminFields is false")
        void shouldNotIncludeAdminFields() {
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .createdAt(now)
                    .lastLoginAt(now.plusDays(1))
                    .role(User.UserRole.FREE)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getCreatedAt()).isNull();
            assertThat(dto.getLastLoginAt()).isNull();
        }
    }

    @Nested
    @DisplayName("fromUser - Trial User")
    class FromUserTrial {

        @Test
        @DisplayName("should include trial info when user is in trial")
        void shouldIncludeTrialInfoWhenInTrial() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime trialEnd = now.plusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.FREE)
                    .trialStartDate(now.minusDays(2))
                    .trialEndDate(trialEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getTrial()).isNotNull();
            assertThat(dto.getTrial().getActive()).isTrue();
            assertThat(dto.getTrial().getStartDate()).isEqualTo(now.minusDays(2));
            assertThat(dto.getTrial().getEndDate()).isEqualTo(trialEnd);
            assertThat(dto.getTrial().getDaysRemaining()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should include inactive trial info for admin when includeAdminFields is true")
        void shouldIncludeInactiveTrialForAdmin() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime pastTrialEnd = now.minusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.FREE)
                    .trialStartDate(now.minusDays(10))
                    .trialEndDate(pastTrialEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, true);

            assertThat(dto.getTrial()).isNotNull();
            assertThat(dto.getTrial().getActive()).isFalse();
        }

        @Test
        @DisplayName("should not include trial info when not in trial and includeAdminFields is false")
        void shouldNotIncludeTrialWhenNotInTrial() {
            LocalDateTime pastTrialEnd = LocalDateTime.now().minusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.FREE)
                    .trialEndDate(pastTrialEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getTrial()).isNull();
        }
    }

    @Nested
    @DisplayName("fromUser - PRO Subscription")
    class FromUserProSubscription {

        @Test
        @DisplayName("should include subscription info for PRO user")
        void shouldIncludeSubscriptionForProUser() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime subEnd = now.plusDays(30);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(now.minusDays(10))
                    .subscriptionEndDate(subEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getSubscription().getActive()).isTrue();
            assertThat(dto.getSubscription().getStartDate()).isEqualTo(now.minusDays(10));
            assertThat(dto.getSubscription().getEndDate()).isEqualTo(subEnd);
            assertThat(dto.getSubscription().getDaysRemaining()).isGreaterThan(0);
            assertThat(dto.getSubscription().getExpired()).isFalse();
        }

        @Test
        @DisplayName("should mark subscription as expired when end date is past")
        void shouldMarkSubscriptionAsExpired() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime pastSubEnd = now.minusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(now.minusDays(40))
                    .subscriptionEndDate(pastSubEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getSubscription().getActive()).isFalse();
            assertThat(dto.getSubscription().getExpired()).isTrue();
        }

        @Test
        @DisplayName("should not include subscription for non-PRO user")
        void shouldNotIncludeSubscriptionForNonProUser() {
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.FREE)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNull();
        }
    }

    @Nested
    @DisplayName("fromUser - Role Expiry Info")
    class FromUserRoleExpiry {

        @Test
        @DisplayName("should include role info when roleExpiryDate is set")
        void shouldIncludeRoleInfoWhenExpiryDateSet() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryDate = now.plusDays(10);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .originalRole(User.UserRole.FREE)
                    .roleExpiryDate(expiryDate)
                    .roleChangeReason("Trial extension")
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getRoleInfo()).isNotNull();
            assertThat(dto.getRoleInfo().getTemporary()).isTrue();
            assertThat(dto.getRoleInfo().getExpiryDate()).isEqualTo(expiryDate);
            assertThat(dto.getRoleInfo().getOriginalRole()).isEqualTo("FREE");
            assertThat(dto.getRoleInfo().getReason()).isEqualTo("Trial extension");
            assertThat(dto.getRoleInfo().getDaysRemaining()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should include admin fields in roleInfo when includeAdminFields is true")
        void shouldIncludeAdminFieldsInRoleInfo() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryDate = now.plusDays(10);
            LocalDateTime changedAt = now.minusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .roleExpiryDate(expiryDate)
                    .roleChangedAt(changedAt)
                    .roleChangedBy("admin@example.com")
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, true);

            assertThat(dto.getRoleInfo()).isNotNull();
            assertThat(dto.getRoleInfo().getChangedAt()).isEqualTo(changedAt);
            assertThat(dto.getRoleInfo().getChangedBy()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("should handle null originalRole in roleInfo")
        void shouldHandleNullOriginalRole() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryDate = now.plusDays(10);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .roleExpiryDate(expiryDate)
                    .originalRole(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getRoleInfo()).isNotNull();
            assertThat(dto.getRoleInfo().getOriginalRole()).isNull();
        }

        @Test
        @DisplayName("should mark role as expired when expiryDate is past")
        void shouldMarkRoleAsExpired() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime pastExpiry = now.minusDays(5);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .roleExpiryDate(pastExpiry)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getRoleInfo()).isNotNull();
            assertThat(dto.getRoleInfo().getExpired()).isTrue();
        }

        @Test
        @DisplayName("should not include roleInfo when roleExpiryDate is null")
        void shouldNotIncludeRoleInfoWhenExpiryDateNull() {
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .roleExpiryDate(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getRoleInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("toMap - Conversion")
    class ToMapConversion {

        @Test
        @DisplayName("should convert DTO to map with all fields")
        void shouldConvertToMapWithAllFields() {
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsEntry("id", "user1");
            assertThat(map).containsEntry("email", "test@example.com");
            assertThat(map).containsEntry("name", "Test User");
            assertThat(map).containsEntry("role", "FREE");
            assertThat(map).containsEntry("effectiveRole", "FREE");
            assertThat(map).containsEntry("isPro", false);
            assertThat(map).containsEntry("isAdmin", false);
        }

        @Test
        @DisplayName("should include trial in map when present")
        void shouldIncludeTrialInMap() {
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("trial");
            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).containsEntry("active", true);
            assertThat(trialMap).containsEntry("daysRemaining", 5L);
        }

        @Test
        @DisplayName("should include subscription in map when present")
        void shouldIncludeSubscriptionInMap() {
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .expired(false)
                    .daysRemaining(30L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("subscription");
            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).containsEntry("active", true);
            assertThat(subMap).containsEntry("expired", false);
            assertThat(subMap).containsEntry("daysRemaining", 30L);
        }

        @Test
        @DisplayName("should include roleInfo in map when present")
        void shouldIncludeRoleInfoInMap() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Trial extension")
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("roleInfo");
            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsEntry("temporary", true);
            assertThat(roleMap).containsEntry("expired", false);
            assertThat(roleMap).containsEntry("originalRole", "FREE");
            assertThat(roleMap).containsEntry("reason", "Trial extension");
        }

        @Test
        @DisplayName("should handle null nested objects in toMap")
        void shouldHandleNullNestedObjectsInToMap() {
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .trial(null)
                    .subscription(null)
                    .roleInfo(null)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).doesNotContainKey("trial");
            assertThat(map).doesNotContainKey("subscription");
            assertThat(map).doesNotContainKey("roleInfo");
        }

        @Test
        @DisplayName("should handle null dates in nested objects")
        void shouldHandleNullDatesInNestedObjects() {
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(null)
                    .endDate(null)
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).doesNotContainKey("startDate");
            assertThat(trialMap).doesNotContainKey("endDate");
            assertThat(trialMap).containsEntry("active", true);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle ADMIN user role")
        void shouldHandleAdminUserRole() {
            User user = User.builder()
                    .id("admin1")
                    .email("admin@example.com")
                    .role(User.UserRole.ADMIN)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getRole()).isEqualTo("ADMIN");
            assertThat(dto.getIsAdmin()).isTrue();
            assertThat(dto.getIsPro()).isTrue(); // Admin is always Pro
        }

        @Test
        @DisplayName("should handle user with all optional fields null")
        void shouldHandleUserWithAllNulls() {
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name(null)
                    .picture(null)
                    .role(User.UserRole.FREE)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getId()).isEqualTo("user1");
            assertThat(dto.getName()).isNull();
            assertThat(dto.getPicture()).isNull();
        }

        @Test
        @DisplayName("should handle PRO user with expired subscription")
        void shouldHandleProUserWithExpiredSubscription() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime pastSubEnd = now.minusDays(10);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(now.minusDays(40))
                    .subscriptionEndDate(pastSubEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getSubscription().getActive()).isFalse();
            assertThat(dto.getSubscription().getExpired()).isTrue();
        }

        @Test
        @DisplayName("should handle PRO user with active subscription")
        void shouldHandleProUserWithActiveSubscription() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime futureSubEnd = now.plusDays(30);
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(now.minusDays(10))
                    .subscriptionEndDate(futureSubEnd)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getSubscription().getActive()).isTrue();
            assertThat(dto.getSubscription().getExpired()).isFalse();
        }

        @Test
        @DisplayName("should handle PRO user with null subscription dates")
        void shouldHandleProUserWithNullSubscriptionDates() {
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .subscriptionStartDate(null)
                    .subscriptionEndDate(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getSubscription().getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromUser - Combined Scenarios")
    class FromUserCombined {

        @Test
        @DisplayName("should handle user with trial, subscription, and role expiry")
        void shouldHandleUserWithAllFeatures() {
            LocalDateTime now = LocalDateTime.now();
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.PRO)
                    .trialStartDate(now.minusDays(5))
                    .trialEndDate(now.plusDays(2))
                    .subscriptionStartDate(now.minusDays(10))
                    .subscriptionEndDate(now.plusDays(20))
                    .roleExpiryDate(now.plusDays(15))
                    .originalRole(User.UserRole.FREE)
                    .roleChangeReason("Trial upgrade")
                    .roleChangedAt(now.minusDays(5))
                    .roleChangedBy("system")
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, true);

            assertThat(dto.getTrial()).isNotNull();
            assertThat(dto.getSubscription()).isNotNull();
            assertThat(dto.getRoleInfo()).isNotNull();
            assertThat(dto.getRoleInfo().getChangedAt()).isNotNull();
            assertThat(dto.getRoleInfo().getChangedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle FREE user with trial but no subscription")
        void shouldHandleFreeUserWithTrialOnly() {
            LocalDateTime now = LocalDateTime.now();
            
            User user = User.builder()
                    .id("user1")
                    .email("test@example.com")
                    .role(User.UserRole.FREE)
                    .trialStartDate(now.minusDays(2))
                    .trialEndDate(now.plusDays(5))
                    .build();

            UserResponseDTO dto = UserResponseDTO.fromUser(user, false);

            assertThat(dto.getTrial()).isNotNull();
            assertThat(dto.getTrial().getActive()).isTrue();
            assertThat(dto.getSubscription()).isNull();
            assertThat(dto.getRoleInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("toMap - All Null Checks")
    class ToMapNullChecks {

        @Test
        @DisplayName("should handle all null fields in toMap")
        void shouldHandleAllNullFields() {
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name(null)
                    .picture(null)
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .createdAt(null)
                    .lastLoginAt(null)
                    .trial(null)
                    .subscription(null)
                    .roleInfo(null)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("id");
            assertThat(map).containsKey("email");
            assertThat(map).doesNotContainKey("createdAt");
            assertThat(map).doesNotContainKey("lastLoginAt");
            assertThat(map).doesNotContainKey("trial");
            assertThat(map).doesNotContainKey("subscription");
            assertThat(map).doesNotContainKey("roleInfo");
        }

        @Test
        @DisplayName("should include all trial fields when all are non-null")
        void shouldIncludeAllTrialFields() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now.minusDays(5))
                    .endDate(now.plusDays(2))
                    .daysRemaining(2L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).containsKey("active");
            assertThat(trialMap).containsKey("startDate");
            assertThat(trialMap).containsKey("endDate");
            assertThat(trialMap).containsKey("daysRemaining");
        }

        @Test
        @DisplayName("should include all subscription fields when all are non-null")
        void shouldIncludeAllSubscriptionFields() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now.minusDays(10))
                    .endDate(now.plusDays(20))
                    .daysRemaining(20L)
                    .expired(false)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).containsKey("active");
            assertThat(subMap).containsKey("startDate");
            assertThat(subMap).containsKey("endDate");
            assertThat(subMap).containsKey("daysRemaining");
            assertThat(subMap).containsKey("expired");
        }

        @Test
        @DisplayName("should include all roleInfo fields when all are non-null")
        void shouldIncludeAllRoleInfoFields() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Trial extension")
                    .changedAt(now.minusDays(5))
                    .changedBy("admin@example.com")
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("temporary");
            assertThat(roleMap).containsKey("expiryDate");
            assertThat(roleMap).containsKey("daysRemaining");
            assertThat(roleMap).containsKey("expired");
            assertThat(roleMap).containsKey("originalRole");
            assertThat(roleMap).containsKey("reason");
            assertThat(roleMap).containsKey("changedAt");
            assertThat(roleMap).containsKey("changedBy");
        }

        @Test
        @DisplayName("should handle partial null fields in trial")
        void shouldHandlePartialNullTrialFields() {
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(null)
                    .endDate(null)
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).containsKey("active");
            assertThat(trialMap).containsKey("daysRemaining");
            assertThat(trialMap).doesNotContainKey("startDate");
            assertThat(trialMap).doesNotContainKey("endDate");
        }

        @Test
        @DisplayName("should handle partial null fields in subscription")
        void shouldHandlePartialNullSubscriptionFields() {
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(null)
                    .endDate(null)
                    .daysRemaining(null)
                    .expired(false)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).containsKey("active");
            assertThat(subMap).containsKey("expired");
            assertThat(subMap).doesNotContainKey("startDate");
            assertThat(subMap).doesNotContainKey("endDate");
            assertThat(subMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle partial null fields in roleInfo")
        void shouldHandlePartialNullRoleInfoFields() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(null)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole(null)
                    .reason(null)
                    .changedAt(null)
                    .changedBy(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("temporary");
            assertThat(roleMap).doesNotContainKey("expiryDate");
            assertThat(roleMap).doesNotContainKey("daysRemaining");
            assertThat(roleMap).doesNotContainKey("expired");
            assertThat(roleMap).doesNotContainKey("originalRole");
            assertThat(roleMap).doesNotContainKey("reason");
            assertThat(roleMap).doesNotContainKey("changedAt");
            assertThat(roleMap).doesNotContainKey("changedBy");
        }

        @Test
        @DisplayName("should include createdAt when not null in toMap")
        void shouldIncludeCreatedAtWhenNotNull() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .createdAt(now)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("createdAt");
            assertThat(map.get("createdAt")).isEqualTo(now);
        }

        @Test
        @DisplayName("should include lastLoginAt when not null in toMap")
        void shouldIncludeLastLoginAtWhenNotNull() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .lastLoginAt(now)
                    .build();

            Map<String, Object> map = dto.toMap();

            assertThat(map).containsKey("lastLoginAt");
            assertThat(map.get("lastLoginAt")).isEqualTo(now);
        }

        @Test
        @DisplayName("should handle trial with only startDate set")
        void shouldHandleTrialWithOnlyStartDate() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(null)
                    .daysRemaining(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).containsKey("startDate");
            assertThat(trialMap).doesNotContainKey("endDate");
            assertThat(trialMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle trial with only endDate set")
        void shouldHandleTrialWithOnlyEndDate() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(null)
                    .endDate(now)
                    .daysRemaining(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .trial(trial)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> trialMap = (Map<String, Object>) map.get("trial");
            assertThat(trialMap).doesNotContainKey("startDate");
            assertThat(trialMap).containsKey("endDate");
            assertThat(trialMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle subscription with only startDate set")
        void shouldHandleSubscriptionWithOnlyStartDate() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(null)
                    .daysRemaining(null)
                    .expired(false)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).containsKey("startDate");
            assertThat(subMap).doesNotContainKey("endDate");
            assertThat(subMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle subscription with only endDate set")
        void shouldHandleSubscriptionWithOnlyEndDate() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(null)
                    .endDate(now)
                    .daysRemaining(null)
                    .expired(false)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).doesNotContainKey("startDate");
            assertThat(subMap).containsKey("endDate");
            assertThat(subMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle roleInfo with only expiryDate set")
        void shouldHandleRoleInfoWithOnlyExpiryDate() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole(null)
                    .reason(null)
                    .changedAt(null)
                    .changedBy(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("expiryDate");
            assertThat(roleMap).doesNotContainKey("daysRemaining");
        }

        @Test
        @DisplayName("should handle roleInfo with only originalRole set")
        void shouldHandleRoleInfoWithOnlyOriginalRole() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(null)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole("FREE")
                    .reason(null)
                    .changedAt(null)
                    .changedBy(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("originalRole");
            assertThat(roleMap.get("originalRole")).isEqualTo("FREE");
        }

        @Test
        @DisplayName("should handle roleInfo with only reason set")
        void shouldHandleRoleInfoWithOnlyReason() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(null)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole(null)
                    .reason("Test reason")
                    .changedAt(null)
                    .changedBy(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("reason");
            assertThat(roleMap.get("reason")).isEqualTo("Test reason");
        }

        @Test
        @DisplayName("should handle roleInfo with only changedAt set")
        void shouldHandleRoleInfoWithOnlyChangedAt() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(null)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole(null)
                    .reason(null)
                    .changedAt(now)
                    .changedBy(null)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("changedAt");
            assertThat(roleMap.get("changedAt")).isEqualTo(now);
        }

        @Test
        @DisplayName("should handle roleInfo with only changedBy set")
        void shouldHandleRoleInfoWithOnlyChangedBy() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(null)
                    .daysRemaining(null)
                    .expired(null)
                    .originalRole(null)
                    .reason(null)
                    .changedAt(null)
                    .changedBy("admin@example.com")
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .roleInfo(roleInfo)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> roleMap = (Map<String, Object>) map.get("roleInfo");
            assertThat(roleMap).containsKey("changedBy");
            assertThat(roleMap.get("changedBy")).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("should handle null expired field in subscription toMap")
        void shouldHandleNullExpiredInSubscription() {
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .expired(null) // Null expired field
                    .daysRemaining(30L)
                    .build();

            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .subscription(subscription)
                    .build();

            Map<String, Object> map = dto.toMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map.get("subscription");
            assertThat(subMap).containsKey("active");
            assertThat(subMap).doesNotContainKey("expired"); // Should not include null expired
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods - TrialInfo")
    class TrialInfoLombokMethods {
        @Test
        @DisplayName("should test TrialInfo equals method")
        void shouldTestTrialInfoEquals() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial1 = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(5))
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO.TrialInfo trial2 = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(5))
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO.TrialInfo trial3 = UserResponseDTO.TrialInfo.builder()
                    .active(false)
                    .startDate(now)
                    .endDate(now.plusDays(5))
                    .daysRemaining(5L)
                    .build();

            assertThat(trial1).isEqualTo(trial2);
            assertThat(trial1).isNotEqualTo(trial3);
            assertThat(trial1).isNotEqualTo(null);
            assertThat(trial1).isNotEqualTo("not a trial");
        }

        @Test
        @DisplayName("should test TrialInfo hashCode method")
        void shouldTestTrialInfoHashCode() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial1 = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(5))
                    .daysRemaining(5L)
                    .build();

            UserResponseDTO.TrialInfo trial2 = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(5))
                    .daysRemaining(5L)
                    .build();

            assertThat(trial1.hashCode()).isEqualTo(trial2.hashCode());
        }

        @Test
        @DisplayName("should test TrialInfo toString method")
        void shouldTestTrialInfoToString() {
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder()
                    .active(true)
                    .daysRemaining(5L)
                    .build();

            String toString = trial.toString();
            assertThat(toString).isNotNull();
            assertThat(toString).contains("TrialInfo");
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods - SubscriptionInfo")
    class SubscriptionInfoLombokMethods {
        @Test
        @DisplayName("should test SubscriptionInfo equals method")
        void shouldTestSubscriptionInfoEquals() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.SubscriptionInfo sub1 = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(30))
                    .daysRemaining(30L)
                    .expired(false)
                    .build();

            UserResponseDTO.SubscriptionInfo sub2 = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(30))
                    .daysRemaining(30L)
                    .expired(false)
                    .build();

            UserResponseDTO.SubscriptionInfo sub3 = UserResponseDTO.SubscriptionInfo.builder()
                    .active(false)
                    .startDate(now)
                    .endDate(now.plusDays(30))
                    .daysRemaining(30L)
                    .expired(true)
                    .build();

            assertThat(sub1).isEqualTo(sub2);
            assertThat(sub1).isNotEqualTo(sub3);
            assertThat(sub1).isNotEqualTo(null);
            assertThat(sub1).isNotEqualTo("not a subscription");
        }

        @Test
        @DisplayName("should test SubscriptionInfo hashCode method")
        void shouldTestSubscriptionInfoHashCode() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.SubscriptionInfo sub1 = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(30))
                    .daysRemaining(30L)
                    .expired(false)
                    .build();

            UserResponseDTO.SubscriptionInfo sub2 = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .startDate(now)
                    .endDate(now.plusDays(30))
                    .daysRemaining(30L)
                    .expired(false)
                    .build();

            assertThat(sub1.hashCode()).isEqualTo(sub2.hashCode());
        }

        @Test
        @DisplayName("should test SubscriptionInfo toString method")
        void shouldTestSubscriptionInfoToString() {
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder()
                    .active(true)
                    .expired(false)
                    .build();

            String toString = subscription.toString();
            assertThat(toString).isNotNull();
            assertThat(toString).contains("SubscriptionInfo");
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods - RoleInfo")
    class RoleInfoLombokMethods {
        @Test
        @DisplayName("should test RoleInfo equals method")
        void shouldTestRoleInfoEquals() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.RoleInfo role1 = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Test")
                    .changedAt(now)
                    .changedBy("admin")
                    .build();

            UserResponseDTO.RoleInfo role2 = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Test")
                    .changedAt(now)
                    .changedBy("admin")
                    .build();

            UserResponseDTO.RoleInfo role3 = UserResponseDTO.RoleInfo.builder()
                    .temporary(false)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Test")
                    .changedAt(now)
                    .changedBy("admin")
                    .build();

            assertThat(role1).isEqualTo(role2);
            assertThat(role1).isNotEqualTo(role3);
            assertThat(role1).isNotEqualTo(null);
            assertThat(role1).isNotEqualTo("not a role");
        }

        @Test
        @DisplayName("should test RoleInfo hashCode method")
        void shouldTestRoleInfoHashCode() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.RoleInfo role1 = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Test")
                    .changedAt(now)
                    .changedBy("admin")
                    .build();

            UserResponseDTO.RoleInfo role2 = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expiryDate(now.plusDays(10))
                    .daysRemaining(10L)
                    .expired(false)
                    .originalRole("FREE")
                    .reason("Test")
                    .changedAt(now)
                    .changedBy("admin")
                    .build();

            assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
        }

        @Test
        @DisplayName("should test RoleInfo toString method")
        void shouldTestRoleInfoToString() {
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder()
                    .temporary(true)
                    .expired(false)
                    .build();

            String toString = roleInfo.toString();
            assertThat(toString).isNotNull();
            assertThat(toString).contains("RoleInfo");
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods - UserResponseDTO")
    class UserResponseDTOLombokMethods {
        @Test
        @DisplayName("should test UserResponseDTO equals method")
        void shouldTestUserResponseDTOEquals() {
            UserResponseDTO dto1 = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            UserResponseDTO dto2 = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            UserResponseDTO dto3 = UserResponseDTO.builder()
                    .id("user2")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1).isNotEqualTo(dto3);
            assertThat(dto1).isNotEqualTo(null);
            assertThat(dto1).isNotEqualTo("not a dto");
        }

        @Test
        @DisplayName("should test UserResponseDTO hashCode method")
        void shouldTestUserResponseDTOHashCode() {
            UserResponseDTO dto1 = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            UserResponseDTO dto2 = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .role("FREE")
                    .effectiveRole("FREE")
                    .isPro(false)
                    .isAdmin(false)
                    .build();

            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("should test UserResponseDTO toString method")
        void shouldTestUserResponseDTOToString() {
            UserResponseDTO dto = UserResponseDTO.builder()
                    .id("user1")
                    .email("test@example.com")
                    .name("Test User")
                    .build();

            String toString = dto.toString();
            assertThat(toString).isNotNull();
            assertThat(toString).contains("UserResponseDTO");
        }

        @Test
        @DisplayName("should test UserResponseDTO getters and setters")
        void shouldTestUserResponseDTOGettersAndSetters() {
            UserResponseDTO dto = new UserResponseDTO();
            LocalDateTime now = LocalDateTime.now();

            dto.setId("user1");
            dto.setEmail("test@example.com");
            dto.setName("Test User");
            dto.setPicture("pic.jpg");
            dto.setRole("FREE");
            dto.setEffectiveRole("FREE");
            dto.setIsPro(false);
            dto.setIsAdmin(false);
            dto.setCreatedAt(now);
            dto.setLastLoginAt(now.plusDays(1));

            assertThat(dto.getId()).isEqualTo("user1");
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getName()).isEqualTo("Test User");
            assertThat(dto.getPicture()).isEqualTo("pic.jpg");
            assertThat(dto.getRole()).isEqualTo("FREE");
            assertThat(dto.getEffectiveRole()).isEqualTo("FREE");
            assertThat(dto.getIsPro()).isFalse();
            assertThat(dto.getIsAdmin()).isFalse();
            assertThat(dto.getCreatedAt()).isEqualTo(now);
            assertThat(dto.getLastLoginAt()).isEqualTo(now.plusDays(1));
        }

        @Test
        @DisplayName("should test UserResponseDTO no-args constructor")
        void shouldTestUserResponseDTONoArgsConstructor() {
            UserResponseDTO dto = new UserResponseDTO();
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isNull();
            assertThat(dto.getEmail()).isNull();
        }

        @Test
        @DisplayName("should test UserResponseDTO all-args constructor")
        void shouldTestUserResponseDTOAllArgsConstructor() {
            LocalDateTime now = LocalDateTime.now();
            UserResponseDTO.TrialInfo trial = UserResponseDTO.TrialInfo.builder().active(true).build();
            UserResponseDTO.SubscriptionInfo subscription = UserResponseDTO.SubscriptionInfo.builder().active(true).build();
            UserResponseDTO.RoleInfo roleInfo = UserResponseDTO.RoleInfo.builder().temporary(true).build();

            UserResponseDTO dto = new UserResponseDTO(
                    "user1", "test@example.com", "Test User", "pic.jpg",
                    "FREE", "FREE", false, false, now, now.plusDays(1),
                    trial, subscription, roleInfo
            );

            assertThat(dto.getId()).isEqualTo("user1");
            assertThat(dto.getTrial()).isEqualTo(trial);
            assertThat(dto.getSubscription()).isEqualTo(subscription);
            assertThat(dto.getRoleInfo()).isEqualTo(roleInfo);
        }
    }
}
