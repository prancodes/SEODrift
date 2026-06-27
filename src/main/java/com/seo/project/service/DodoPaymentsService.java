package com.seo.project.service;

import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionRequest;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionResponse;
import com.dodopayments.api.models.checkoutsessions.ProductItemReq;
import com.dodopayments.api.models.subscriptions.Subscription;
import com.dodopayments.api.models.subscriptions.SubscriptionStatus;
import com.dodopayments.api.models.subscriptions.SubscriptionUpdateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DodoPaymentsService {

    @Value("${dodo.payments.product.pro-id:}")
    private String proProductId;

    @Value("${app.base-url}")
    private String baseUrl;

    private final DodoPaymentsClient client;

    public DodoPaymentsService(@Autowired(required = false) DodoPaymentsClient client) {
        this.client = client;
    }

    public String createCheckoutSession(String userEmail) {
        if (this.client == null) {
            log.error("Dodo client is null. Cannot create checkout session.");
            return baseUrl + "/pro?error=payment_not_configured";
        }
        try {
            CheckoutSessionRequest params = CheckoutSessionRequest.builder()
                .addProductCart(ProductItemReq.builder()
                    .productId(proProductId)
                    .quantity(1)
                    .build())
                .returnUrl(baseUrl + "/dodo/success")
                .build();

            CheckoutSessionResponse response = client.checkoutSessions().create(params);
            return response.checkoutUrl().orElseThrow(() -> new RuntimeException("Checkout URL not returned by Dodo"));
        } catch (Exception e) {
            log.error("Failed to create Dodo Payments checkout session", e);
            throw new RuntimeException("Could not create payment session");
        }
    }

    /**
     * Verifies a subscription ID is legitimate by querying the Dodo Payments API.
     * Returns true only if the subscription exists and has an active or paused status.
     *
     * This prevents self-upgrade attacks where a user manually hits /dodo/success
     * with a fake subscription_id parameter. We call Dodo server-side to confirm
     * the payment actually happened before granting ROLE_PRO.
     */
    public boolean verifySubscription(String subscriptionId) {
        if (this.client == null) {
            log.warn("Dodo client not configured — cannot verify subscription.");
            return false;
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            log.warn("Subscription verification failed: blank subscription ID.");
            return false;
        }
        try {
            Subscription subscription = client.subscriptions().retrieve(subscriptionId);
            String status = subscription.status().toString().toLowerCase();
            boolean valid = "active".equals(status) || "paused".equals(status);
            if (!valid) {
                log.warn("Subscription {} has non-active status: {}", subscriptionId, status);
            }
            return valid;
        } catch (Exception e) {
            log.error("Failed to verify subscription {} with Dodo: {}", subscriptionId, e.getMessage());
            return false;
        }
    }

    /**
     * Cancels an active subscription programmatically with Dodo Payments.
     */
    public boolean cancelSubscription(String subscriptionId) {
        if (this.client == null) {
            log.warn("Dodo client not configured — cannot cancel subscription.");
            return false;
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            log.warn("Subscription cancellation failed: blank subscription ID.");
            return false;
        }
        try {
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .subscriptionId(subscriptionId)
                    .status(SubscriptionStatus.CANCELLED)
                    .build();
            client.subscriptions().update(params);
            log.info("Successfully cancelled Dodo subscription: {}", subscriptionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel subscription {} in Dodo: {}", subscriptionId, e.getMessage(), e);
            return false;
        }
    }
}
