package com.seo.project.config;

import com.seo.project.service.CustomOAuth2UserService;
import com.seo.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * SecurityConfig orchestrates the application's security posture,
 * defining access rules, OAuth2/OIDC integration, and public/private routing.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public SecurityConfig() {
        log.info("Security Infrastructure Initialized.");
    }

    /**
     * Bean definition for our specialized OAuth2/OIDC service.
     * Manual creation ensures we can inject the UserRepository for database
     * synchronization.
     */
    @Bean
    public CustomOAuth2UserService customOAuth2UserService(@Lazy UserService userService) {
        log.debug("Provisioning CustomOAuth2UserService with UserService injection.");
        return new CustomOAuth2UserService(userService);
    }

    /**
     * Defines the primary security filter chain for the application.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            CustomOAuth2UserService customOAuth2UserService,
            ClientRegistrationRepository clientRegistrationRepository)
            throws Exception {
        log.info("Configuring Security Filter Chain...");

        OAuth2AuthorizationRequestResolver resolver = authorizationRequestResolver(clientRegistrationRepository);

        http
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible assets and home page
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicons/**",
                                "/api/auth/**", "/robots.txt", "/sitemap.xml")
                        .permitAll()

                        // Protected tools and user data
                        .requestMatchers("/analytics/**", "/dashboard/**", "/tags/**", "/thumbnail/**",
                                "/workspace", "/workspace/**", "/history", "/history/**",
                                "/api/gateway/youtube/**")
                        .authenticated()

                        // Everything else remains public for flexibility
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(resolver))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOAuth2UserService::loadOidcUser))
                        .successHandler((request, response, authentication) -> {
                            String redirectUrl = null;
                            Cookie[] cookies = request.getCookies();
                            if (cookies != null) {
                                for (Cookie cookie : cookies) {
                                    if ("seodrift_login_redirect".equals(cookie.getName())) {
                                        redirectUrl = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                                        break;
                                    }
                                }
                            }

                            // Clear cookie
                            Cookie clearCookie = new Cookie("seodrift_login_redirect", null);
                            clearCookie.setPath("/");
                            clearCookie.setMaxAge(0);
                            response.addCookie(clearCookie);

                            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                                log.info("OAuth2 login successful. Redirecting directly to: {}", redirectUrl);
                                response.sendRedirect(redirectUrl);
                                return;
                            }

                            RequestCache requestCache = new HttpSessionRequestCache();
                            SavedRequest savedRequest = requestCache.getRequest(request, response);
                            if (savedRequest != null) {
                                String targetUrl = savedRequest.getRedirectUrl();
                                log.info("OAuth2 login successful. Redirecting to intercepted URL: {}", targetUrl);
                                response.sendRedirect(targetUrl);
                                return;
                            }

                            response.sendRedirect("/");
                        }))
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());

        log.info("Security Configuration successfully applied.");
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                .additionalParameters(params -> {
                    params.put("access_type", "offline");
                    params.put("prompt", "consent");
                })
        );
        return resolver;
    }
}
