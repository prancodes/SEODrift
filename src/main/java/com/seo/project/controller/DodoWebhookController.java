package com.seo.project.controller;

import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.core.UnwrapWebhookParams;
import com.dodopayments.api.core.http.Headers;
import com.dodopayments.api.models.subscriptions.Subscription;
import com.dodopayments.api.models.webhooks.*;
import com.seo.project.model.User;
import com.seo.project.model.Notification;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class DodoWebhookController {

    private final DodoPaymentsClient dodoPaymentsClient;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Value("${dodo.payments.webhook.secret:}")
    private String webhookSecret;

    public DodoWebhookController(@Autowired(required = false) DodoPaymentsClient dodoPaymentsClient, 
                                 UserRepository userRepository,
                                 NotificationRepository notificationRepository) {
        this.dodoPaymentsClient = dodoPaymentsClient;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/api/dodo/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String body,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp) {

        log.info("Received Dodo Payments webhook event. ID: {}, Timestamp: {}", webhookId, webhookTimestamp);

        if (dodoPaymentsClient == null) {
            log.error("Dodo payments client is not configured. Webhook processing disabled.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dodo client not configured");
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Dodo webhook secret is not configured. Webhook processing disabled.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
        }

        if (webhookId == null || webhookSignature == null || webhookTimestamp == null) {
            log.warn("Missing required webhook headers. ID: {}, Signature: {}, Timestamp: {}", webhookId, webhookSignature, webhookTimestamp);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing webhook signature headers");
        }

        try {
            Headers headers = Headers.builder()
                    .put("webhook-id", webhookId)
                    .put("webhook-signature", webhookSignature)
                    .put("webhook-timestamp", webhookTimestamp)
                    .build();

            UnwrapWebhookParams params = UnwrapWebhookParams.builder()
                    .body(body)
                    .headers(headers)
                    .secret(webhookSecret)
                    .build();

            UnwrapWebhookEvent event = dodoPaymentsClient.webhooks().unwrap(params);

            if (event.isSubscriptionActive()) {
                SubscriptionActiveWebhookEvent activeEvent = event.asSubscriptionActive();
                Subscription subscription = activeEvent.data();
                log.info("Processing subscription.active event for ID: {}", subscription.subscriptionId());
                updateUserSubscription(subscription.customer().email(), subscription.subscriptionId(), "ROLE_PRO");
            } else if (event.isSubscriptionRenewed()) {
                SubscriptionRenewedWebhookEvent renewedEvent = event.asSubscriptionRenewed();
                Subscription subscription = renewedEvent.data();
                log.info("Processing subscription.renewed event for ID: {}", subscription.subscriptionId());
                updateUserSubscription(subscription.customer().email(), subscription.subscriptionId(), "ROLE_PRO");
            } else if (event.isSubscriptionCancelled()) {
                SubscriptionCancelledWebhookEvent cancelledEvent = event.asSubscriptionCancelled();
                Subscription subscription = cancelledEvent.data();
                log.info("Processing subscription.cancelled event for ID: {}", subscription.subscriptionId());
                updateUserSubscription(subscription.customer().email(), subscription.subscriptionId(), "ROLE_USER");
            } else if (event.isSubscriptionExpired()) {
                SubscriptionExpiredWebhookEvent expiredEvent = event.asSubscriptionExpired();
                Subscription subscription = expiredEvent.data();
                log.info("Processing subscription.expired event for ID: {}", subscription.subscriptionId());
                updateUserSubscription(subscription.customer().email(), subscription.subscriptionId(), "ROLE_USER");
            } else if (event.isSubscriptionFailed()) {
                SubscriptionFailedWebhookEvent failedEvent = event.asSubscriptionFailed();
                Subscription subscription = failedEvent.data();
                log.info("Processing subscription.failed event for ID: {}", subscription.subscriptionId());
                updateUserSubscription(subscription.customer().email(), subscription.subscriptionId(), "ROLE_USER");
            } else {
                log.info("Ignoring non-subscription webhook event type.");
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error verifying or processing webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature or payload");
        }
    }

    private void updateUserSubscription(String email, String subscriptionId, String role) {
        if (email == null || email.isBlank()) {
            log.warn("Webhook subscription update failed: blank email address.");
            return;
        }
        synchronized (email.intern()) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if ("ROLE_PRO".equals(role)) {
                    boolean wasPro = "ROLE_PRO".equals(user.getRole());
                    
                    // Duplicate event guard: if user already has this subscription and is PRO, skip processing
                    if (subscriptionId != null && subscriptionId.equals(user.getDodoSubscriptionId()) && wasPro) {
                        log.info("User {} is already upgraded to ROLE_PRO with sub ID: {}. Skipping duplicate webhook.", email, subscriptionId);
                        return;
                    }
                    
                    user.setDodoSubscriptionId(subscriptionId);
                    user.setRole("ROLE_PRO");
                    userRepository.save(user);
                    log.info("Upgraded user {} to ROLE_PRO with sub ID: {}", email, subscriptionId);

                    // Add in-app notification if user was not already PRO
                    if (!wasPro) {
                        Notification notification = Notification.builder()
                                .user(user)
                                .title("🎉 PRO Upgrade Successful!")
                                .message("Thank you for upgrading! You now have unlimited saved keywords, Gemini AI SEO analysis, competitor gap analysis, and advanced analytics.")
                                .type("BILLING_UPGRADE")
                                .isRead(false)
                                .build();
                        notificationRepository.save(notification);
                    }
                } else {
                    // Race condition guard: only downgrade if the webhook cancellation matches the active subscription in our DB
                    if (subscriptionId == null || subscriptionId.equals(user.getDodoSubscriptionId())) {
                        user.setDodoSubscriptionId(null);
                        user.setRole("ROLE_USER");
                        userRepository.save(user);
                        log.info("Downgraded user {} to ROLE_USER from sub ID: {}", email, subscriptionId);

                        Notification notification = Notification.builder()
                                .user(user)
                                .title("⚠️ Subscription Inactive")
                                .message("Your PRO subscription is no longer active. You have been placed back on the Free tier.")
                                .type("BILLING_DOWNGRADE")
                                .isRead(false)
                                .build();
                        notificationRepository.save(notification);
                    } else {
                        log.info("Ignored downgrade event for user {}: sub ID {} doesn't match active sub ID {}", 
                                email, subscriptionId, user.getDodoSubscriptionId());
                    }
                }
            } else {
                log.info("Webhook user lookup failed for email: {} (This is normal if the user recently deleted their account).", email);
            }
        }
    }
}
