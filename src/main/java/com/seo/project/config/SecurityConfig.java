package com.seo.project.config;

import com.seo.project.service.CustomOAuth2UserService;
import com.seo.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
    public CustomOAuth2UserService customOAuth2UserService(UserService userService) {
        log.debug("Provisioning CustomOAuth2UserService with UserService injection.");
        return new CustomOAuth2UserService(userService);
    }

    /**
     * Defines the primary security filter chain for the application.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService)
            throws Exception {
        log.info("Configuring Security Filter Chain...");

        http
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible assets and home page
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicons/**",
                                "/api/auth/**", "/robots.txt", "/sitemap.xml")
                        .permitAll()

                        // Protected tools and user data
                        .requestMatchers("/analytics/**", "/dashboard/**", "/tags/**", "/thumbnail/**",
                                "/api/gateway/youtube/**")
                        .authenticated()

                        // Everything else remains public for flexibility
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOAuth2UserService::loadOidcUser))
                        .defaultSuccessUrl("/", false))
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());

        log.info("Security Configuration successfully applied.");
        return http.build();
    }
}
