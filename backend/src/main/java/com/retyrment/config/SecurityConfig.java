package com.retyrment.config;

import com.retyrment.security.JwtAuthenticationFilter;
import com.retyrment.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final com.retyrment.security.OAuth2RedirectStateStore redirectStateStore;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration corsConfiguration = new CorsConfiguration();
                corsConfiguration.setAllowedOrigins(List.of(
                    "http://localhost:3000", "http://127.0.0.1:3000",
                    "http://localhost:3001", "http://127.0.0.1:3001",
                    "http://localhost:3002", "http://127.0.0.1:3002",
                    "http://localhost:5000", "http://127.0.0.1:5000",
                    "https://retyrment.com", "https://www.retyrment.com",
                    "http://retyrment.com", "http://www.retyrment.com"
                ));
                corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfiguration.setAllowedHeaders(List.of("*"));
                corsConfiguration.setAllowCredentials(true);
                corsConfiguration.setExposedHeaders(List.of("Authorization"));
                return corsConfiguration;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no auth required
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/**").permitAll()
                .requestMatchers("/health/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authz -> authz
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestResolver(oAuth2AuthorizationRequestResolver())
                )
                .redirectionEndpoint(redirect -> redirect
                    .baseUri("/login/oauth2/code/*")
                )
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver() {
        return new com.retyrment.security.OAuth2RedirectAuthorizationRequestResolver(
            clientRegistrationRepository,
            "/oauth2/authorization",
            redirectStateStore,
            allowedOrigins
        );
    }

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;
}
