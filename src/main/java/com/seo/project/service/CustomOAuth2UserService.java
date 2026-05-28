package com.seo.project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomOAuth2UserService manages user identity synchronization between OAuth2/OIDC providers
 * (like Google) and the local PostgreSQL database.
 */
@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService oauth2Delegate = new DefaultOAuth2UserService();
    private final OidcUserService oidcDelegate = new OidcUserService();

    /**
     * Constructor injection for required services.
     */
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Entry point for standard OAuth2 authentication requests.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.debug("OAuth2 authentication handshake initiated.");
        OAuth2User oauth2User = oauth2Delegate.loadUser(userRequest);
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");
        String pictureUrl = oauth2User.getAttribute("picture");
        
        userService.processAndSyncUser(email, name, googleId, pictureUrl);
        
        return oauth2User;
    }

    /**
     * Specialized entry point for OpenID Connect (OIDC) requests, typical for Google logins.
     * Maps the OidcUserRequest to our internal user synchronization logic.
     */
    @Transactional
    public OidcUser loadOidcUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.debug("OIDC authentication handshake initiated.");
        OidcUser oidcUser = oidcDelegate.loadUser(userRequest);
        
        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");
        String googleId = oidcUser.getAttribute("sub");
        String pictureUrl = oidcUser.getAttribute("picture");
        
        userService.processAndSyncUser(email, name, googleId, pictureUrl);
        
        return oidcUser;
    }
}
