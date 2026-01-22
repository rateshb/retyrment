package com.retyrment.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OAuth2RedirectAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String REDIRECT_PARAM = "redirect";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;
    private final OAuth2RedirectStateStore redirectStateStore;
    private final String allowedOrigins;

    public OAuth2RedirectAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        String authorizationRequestBaseUri,
        OAuth2RedirectStateStore redirectStateStore,
        String allowedOrigins
    ) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, authorizationRequestBaseUri
        );
        this.redirectStateStore = redirectStateStore;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return storeRedirectIfPresent(request, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return storeRedirectIfPresent(request, authorizationRequest);
    }

    private OAuth2AuthorizationRequest storeRedirectIfPresent(
        HttpServletRequest request,
        OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        String redirect = request.getParameter(REDIRECT_PARAM);
        String origin = extractAllowedOrigin(redirect);
        if (origin != null) {
            redirectStateStore.put(authorizationRequest.getState(), origin);
        }

        return authorizationRequest;
    }

    private String extractAllowedOrigin(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return null;
        }

        URI uri;
        try {
            uri = URI.create(redirect);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            return null;
        }

        String origin = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        List<String> allowed = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        return allowed.contains(origin) ? origin : null;
    }
}
