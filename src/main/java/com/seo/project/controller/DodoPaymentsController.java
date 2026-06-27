package com.seo.project.controller;

import com.seo.project.model.User;
import com.seo.project.repository.UserRepository;
import com.seo.project.service.DodoPaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Slf4j
@Controller
public class DodoPaymentsController {

    private final DodoPaymentsService dodoPaymentsService;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public DodoPaymentsController(DodoPaymentsService dodoPaymentsService, UserRepository userRepository) {
        this.dodoPaymentsService = dodoPaymentsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pro")
    public String proPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                model.addAttribute("user", userOpt.get());
                if ("ROLE_PRO".equals(userOpt.get().getRole())) {
                    model.addAttribute("isPro", true);
                }
            }
        }
        return "pro"; // Needs a simple pro.html page
    }

    @GetMapping("/dodo/checkout")
    public RedirectView checkout(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return new RedirectView("/");
        }

        String email = oauth2User.getAttribute("email");
        String url = dodoPaymentsService.createCheckoutSession(email);
        return new RedirectView(url);
    }

    @GetMapping("/dodo/success")
    public String success(@RequestParam(value = "subscription_id", required = false) String subscriptionId,
                          @RequestParam(value = "status", required = false) String status,
                          Authentication authentication,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            // Security: verify the subscription_id with Dodo server-side before upgrading.
            // This prevents self-upgrade by manually hitting /dodo/success?subscription_id=fake
            if (!dodoPaymentsService.verifySubscription(subscriptionId)) {
                log.warn("Subscription verification failed for user {} with ID {}. Upgrade denied.", email, subscriptionId);
                return "redirect:/error?reason=payment_failed";
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setDodoSubscriptionId(subscriptionId);
                user.setRole("ROLE_PRO");
                userRepository.save(user);
                log.info("Upgraded user {} to ROLE_PRO with subscription {}", email, subscriptionId);

                // Update Security Context to reflect the new role immediately
                if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                    List<GrantedAuthority> updatedAuthorities = new ArrayList<>(oauthToken.getAuthorities());
                    updatedAuthorities.removeIf(auth -> auth.getAuthority().equals("ROLE_USER"));
                    updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_PRO"));

                    OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                            oauthToken.getPrincipal(),
                            updatedAuthorities,
                            oauthToken.getAuthorizedClientRegistrationId());

                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                }
            }
        }
        return "redirect:/"; // Redirect to dashboard
    }
}
