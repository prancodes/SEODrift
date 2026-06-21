package com.seo.project.config;

import com.seo.project.service.CustomOAuth2UserService;
import com.seo.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
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

    @Value("${app.security.oauth2.encrypt-key}")
    private String encryptKey;

    @Value("${app.security.oauth2.encrypt-salt}")
    private String encryptSalt;

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
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository)
            throws Exception {
        log.info("Configuring Security Filter Chain...");

        OAuth2AuthorizationRequestResolver resolver = authorizationRequestResolver(clientRegistrationRepository);

        http
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "font-src 'self'; " +
                                        "img-src 'self' data: https://img.youtube.com https://i.ytimg.com https://*.googleusercontent.com https://ui-avatars.com https://images.unsplash.com; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'none';"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true))
                        .crossOriginOpenerPolicy(coop -> coop
                                .policy(org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN))
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible assets and home page
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicons/**",
                                "/robots.txt", "/sitemap.xml")
                        .permitAll()

                        // Protected tools and user data
                        .requestMatchers("/analytics/**", "/dashboard/**", "/tags/**", "/thumbnail/**",
                                "/trends", "/trends/**", "/keywords", "/keywords/**",
                                "/workspace", "/workspace/**", "/history", "/history/**",
                                "/api/gateway/youtube/**")
                        .authenticated()

                        // Everything else remains public for flexibility
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientRepository(authorizedClientRepository)
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
    
    /**
     * Dedicated high-priority filter chain for static assets.
     * Bypasses heavy security filters (session creation, security context, csrf)
     * while retaining standard security headers (HSTS, etc.) without triggering
     * Spring Security warnings.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring High-Priority Security Filter Chain for Static Resources...");
        http
                .securityMatcher(
                        "/dist/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicons/**",
                        "/robots.txt",
                        "/sitemap.xml",
                        "/favicon.ico"
                )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .requestCache(requestCache -> requestCache.disable())
                .securityContext(securityContext -> securityContext.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.cacheControl(cache -> cache.disable()));
        return http.build();
    }

    @Bean
    public TextEncryptor textEncryptor() {
        log.info("Initializing Deluxe AES-GCM TextEncryptor for OAuth2 Tokens...");
        return Encryptors.delux(encryptKey, encryptSalt);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            JdbcTemplate jdbcTemplate,
            ClientRegistrationRepository clientRegistrationRepository,
            TextEncryptor textEncryptor) {
        JdbcOAuth2AuthorizedClientService jdbcService = new JdbcOAuth2AuthorizedClientService(jdbcTemplate,
                clientRegistrationRepository);
        return new EncryptedOAuth2AuthorizedClientService(jdbcService, textEncryptor);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                .additionalParameters(params -> {
                    params.put("access_type", "offline");
                }));
        return resolver;
    }
}
