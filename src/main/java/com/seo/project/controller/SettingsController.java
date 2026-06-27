package com.seo.project.controller;

import com.seo.project.model.User;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.UserChannelSnapshotRepository;
import com.seo.project.service.DodoPaymentsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Controller
public class SettingsController {

    private final UserRepository userRepository;
    private final UserChannelSnapshotRepository userChannelSnapshotRepository;
    private final DodoPaymentsService dodoPaymentsService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public SettingsController(UserRepository userRepository, 
                              UserChannelSnapshotRepository userChannelSnapshotRepository,
                              DodoPaymentsService dodoPaymentsService,
                              OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.userChannelSnapshotRepository = userChannelSnapshotRepository;
        this.dodoPaymentsService = dodoPaymentsService;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/settings")
    public String showSettings(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return "redirect:/";
        }
        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            model.addAttribute("isPro", "ROLE_PRO".equals(userOpt.get().getRole()));
        } else {
            return "redirect:/";
        }
        return "settings";
    }

    @PostMapping("/settings/toggle-email-notifications")
    @ResponseBody
    public ResponseEntity<?> toggleEmailNotifications(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean currentState = user.getEmailNotificationsEnabled() != null ? user.getEmailNotificationsEnabled() : true;
                user.setEmailNotificationsEnabled(!currentState);
                userRepository.save(user);
                return ResponseEntity.ok().body(Map.of("enabled", !currentState));
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @Transactional
    @PostMapping("/settings/delete-account")
    public String deleteAccount(Authentication authentication, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("Deleting account for user: {}", email);
                
                // Cancel active Dodo subscription programmatically before deleting account data
                String subId = user.getDodoSubscriptionId();
                if (subId != null && !subId.isBlank()) {
                    log.info("Active subscription {} found for user {}. Triggering Dodo cancellation...", subId, email);
                    boolean cancelled = dodoPaymentsService.cancelSubscription(subId);
                    if (!cancelled) {
                        log.warn("Dodo subscription cancellation failed for sub ID {}. Proceeding with account deletion.", subId);
                    }
                }
                
                user.getCompetitorChannels().clear(); // Clear mapping
                userChannelSnapshotRepository.deleteAllByUser(user);
                userRepository.delete(user);
                
                // Clean up OAuth2 credentials from oauth2_authorized_client table
                try {
                    authorizedClientService.removeAuthorizedClient("google", email);
                    log.info("Successfully cleaned up OAuth2 authorized client tokens for user: {}", email);
                } catch (Exception e) {
                    log.error("Failed to remove OAuth2 authorized client tokens", e);
                }
                
                try {
                    request.logout();
                } catch (Exception e) {
                    log.error("Error logging out user during deletion", e);
                }
                
                redirectAttributes.addFlashAttribute("message", "Your account and all associated data have been permanently deleted.");
            }
        }
        return "redirect:/";
    }

    @PostMapping("/settings/cancel-subscription")
    public String cancelSubscription(Authentication authentication, 
                                     RedirectAttributes redirectAttributes, 
                                     HttpServletRequest request, 
                                     HttpServletResponse response) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String subId = user.getDodoSubscriptionId();
                if (subId != null && !subId.isBlank()) {
                    log.info("Request to cancel subscription {} for user {}", subId, email);
                    boolean cancelled = dodoPaymentsService.cancelSubscription(subId);
                    if (cancelled) {
                        user.setDodoSubscriptionId(null);
                        user.setRole("ROLE_USER");
                        userRepository.save(user);

                        // Dynamically refresh current session authorities from ROLE_PRO to ROLE_USER
                        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                            List<GrantedAuthority> updatedAuthorities = new ArrayList<>(oauthToken.getAuthorities());
                            updatedAuthorities.removeIf(auth -> auth.getAuthority().equals("ROLE_PRO"));
                            if (updatedAuthorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
                                updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            }

                            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                                    oauthToken.getPrincipal(),
                                    updatedAuthorities,
                                    oauthToken.getAuthorizedClientRegistrationId());

                            SecurityContextHolder.getContext().setAuthentication(newAuth);
                            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                        }

                        redirectAttributes.addFlashAttribute("message", "Your subscription has been successfully cancelled. You have been downgraded to basic access.");
                    } else {
                        redirectAttributes.addFlashAttribute("error", "Failed to cancel subscription via Dodo Payments. Please try again or contact support.");
                    }
                } else {
                    redirectAttributes.addFlashAttribute("error", "No active subscription found to cancel.");
                }
            }
        }
        return "redirect:/settings";
    }
}
