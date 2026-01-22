package com.retyrment.security;

import com.retyrment.model.User;
import com.retyrment.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final OAuth2RedirectStateStore redirectStateStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    @Value("${app.admin.emails}")
    private String adminEmails;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");
        
        // Check if email is in admin list
        List<String> adminEmailList = Arrays.asList(adminEmails.split(","));
        boolean isAdmin = adminEmailList.stream()
                .map(String::trim)
                .anyMatch(e -> e.equalsIgnoreCase(email));
        
        // Find or create user
        LocalDateTime now = LocalDateTime.now();
        
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User.UserBuilder builder = User.builder()
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .providerId(providerId)
                    .provider("google")
                    .role(isAdmin ? User.UserRole.ADMIN : User.UserRole.FREE)
                    .createdAt(now);
                
                // Grant 7-day PRO trial to new non-admin users
                if (!isAdmin) {
                    builder.trialStartDate(now);
                    builder.trialEndDate(now.plusDays(7));
                }
                
                return builder.build();
            });
        
        // Update user info (but don't downgrade admin)
        user.setName(name);
        user.setPicture(picture);
        user.setLastLoginAt(LocalDateTime.now());
        
        // Auto-upgrade to admin if in admin list
        if (isAdmin && user.getRole() != User.UserRole.ADMIN) {
            user.setRole(User.UserRole.ADMIN);
        }
        
        userRepository.save(user);
        
        // Generate JWT with role
        String token = jwtUtils.generateToken(email, name, user.getRole().name());
        
        // Redirect to frontend with token
        String redirectBase = frontendUrl;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object redirectOrigin = session.getAttribute("oauth2_redirect_origin");
            if (redirectOrigin instanceof String origin && !origin.isBlank()) {
                redirectBase = decodeOrigin(origin);
                session.removeAttribute("oauth2_redirect_origin");
            }
        }

        String state = request.getParameter("state");
        if (state != null && !state.isBlank()) {
            String stateOrigin = redirectStateStore.getAndRemove(state);
            if (stateOrigin != null && !stateOrigin.isBlank()) {
                redirectBase = stateOrigin;
            }
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("oauth2_redirect_origin".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    redirectBase = decodeOrigin(cookie.getValue());
                    Cookie clear = new Cookie("oauth2_redirect_origin", "");
                    clear.setPath("/");
                    clear.setMaxAge(0);
                    response.addCookie(clear);
                    break;
                }
            }
        }

        String redirectUrl = redirectBase + "/?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String decodeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return origin;
        }
        try {
            return URLDecoder.decode(origin, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return origin;
        }
    }
}
