package com.toollix.subscriptions;

import com.toollix.subscriptions.model.Subscription;
import com.toollix.subscriptions.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionsController {

    private final SubscriptionService service;

    public SubscriptionsController(SubscriptionService service) {
        this.service = service;
    }

    /**
     * POST /subscriptions/checkout
     * Returns { checkout_url, subscription_id } so the frontend can pass sub_id in
     * the callback.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@AuthenticationPrincipal String principal,
            @RequestBody CheckoutRequest req) {
        Long userId = Long.parseLong(principal);
        SubscriptionService.CheckoutResult result = service.initializeCheckout(userId, req.email(), req.planId(),
                req.price(), req.orgId());
        return ResponseEntity.ok(Map.of(
                "checkout_url", result.checkoutUrl(),
                "subscription_id", result.subscriptionId()));
    }

    /**
     * POST /subscriptions/{subscriptionId}/verify?txRef=...
     * Called by the frontend after Chapa redirects back.
     */
    @PostMapping("/{subscriptionId}/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable("subscriptionId") Long subscriptionId,
            @RequestParam("txRef") String txRef) {
        boolean success = service.verifyCheckout(txRef, subscriptionId);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Payment verified and subscription activated!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Payment verification failed or pending."));
        }
    }

    /**
     * GET /subscriptions/my — returns all subscriptions for the authenticated user
     */
    @GetMapping("/my")
    public ResponseEntity<?> mySubscriptions(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    /**
     * GET /subscriptions/org/{orgId} — returns all subscriptions for an
     * organization
     * (We omit strict checking here for simplicity, but in production, we should
     * check admin access)
     */
    @GetMapping("/org/{orgId}")
    public ResponseEntity<?> orgSubscriptions(@PathVariable("orgId") Long orgId) {
        return ResponseEntity.ok(service.getByOrgId(orgId));
    }

    /** Admin: manually create a subscription */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Subscription s) {
        return ResponseEntity.ok(service.create(s));
    }

    public record CheckoutRequest(Long planId, Double price, String email, Long orgId) {
    }
}
