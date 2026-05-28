package com.seo.project.controller.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.seo.project.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> authenticate(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String idTokenString = payload.get("token");
        if (idTokenString == null) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();

                String email = googlePayload.getEmail();
                String name = (String) googlePayload.get("name");
                String googleId = googlePayload.getSubject();
                String pictureUrl = (String) googlePayload.get("picture");

                log.info("Successfully verified Google ID Token for: {}", email);

                // Sync user with database
                userService.processAndSyncUser(email, name, googleId, pictureUrl);

                // Create Spring Security Authentication
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                
                // Construct OidcUser to match standard Spring Security OAuth2 behavior
                OidcIdToken oidcIdToken = new OidcIdToken(idTokenString, 
                        googlePayload.getIssuedAtTimeSeconds() != null ? java.time.Instant.ofEpochSecond(googlePayload.getIssuedAtTimeSeconds()) : java.time.Instant.now(), 
                        googlePayload.getExpirationTimeSeconds() != null ? java.time.Instant.ofEpochSecond(googlePayload.getExpirationTimeSeconds()) : java.time.Instant.now().plusSeconds(3600), 
                        googlePayload);
                
                OidcUser oidcUser = new DefaultOidcUser(authorities, oidcIdToken, "name");
                
                Authentication authentication = new OAuth2AuthenticationToken(oidcUser, authorities, "google");
                
                // Set in SecurityContext
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                
                // Persist session
                HttpSession session = request.getSession(true);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

                return ResponseEntity.ok(Map.of("status", "success", "message", "Authenticated successfully"));
            } else {
                log.warn("Invalid ID Token received");
                return ResponseEntity.status(401).body("Invalid ID Token");
            }
        } catch (Exception e) {
            log.error("Error during Google token verification", e);
            return ResponseEntity.status(500).body("Internal authentication error");
        }
    }
}
