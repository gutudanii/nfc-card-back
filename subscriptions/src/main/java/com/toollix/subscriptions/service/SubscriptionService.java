package com.toollix.subscriptions.service;

import com.toollix.subscriptions.model.Subscription;
import com.toollix.subscriptions.repo.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository repo;
    private final ChapaPaymentService chapaPaymentService;
    private final com.toollix.common.audit.AuditService auditService;

    public SubscriptionService(SubscriptionRepository repo,
            ChapaPaymentService chapaPaymentService,
            com.toollix.common.audit.AuditService auditService) {
        this.repo = repo;
        this.chapaPaymentService = chapaPaymentService;
        this.auditService = auditService;
    }

    /**
     * Initiates a Chapa checkout. Creates a PENDING subscription with the txRef
     * so the callback can look it up by txRef later.
     */
    public CheckoutResult initializeCheckout(Long userId, String email, Long planId, Double expectedPrice, Long orgId) {
        log.info("[SUBSCRIPTION_SVC] initializeCheckout userId={} planId={} orgId={}", userId, planId, orgId);

        String txRef = "NFC-SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        String subjectType = (orgId != null) ? "ORG" : "USER";
        Long subjectId = (orgId != null) ? orgId : userId;

        // Cancel any existing PENDING subscriptions for this subject to avoid clutter
        List<Subscription> existing = repo.findBySubjectTypeAndSubjectIdOrderByIdDesc(subjectType, subjectId);
        for (Subscription sub : existing) {
            if ("PENDING".equals(sub.getStatus())) {
                sub.setStatus("CANCELLED");
                repo.save(sub);
            }
        }

        Subscription s = new Subscription();
        s.setSubjectType(subjectType);
        s.setSubjectId(subjectId);
        s.setPlanId(planId);
        s.setTxRef(txRef);
        s.setStatus("PENDING");
        s = repo.save(s);

        try {
            String checkoutUrl = chapaPaymentService.generateChapaPaymentLink(email, expectedPrice, txRef, s.getId());
            return new CheckoutResult(s.getId(), checkoutUrl);
        } catch (Exception e) {
            log.error("[SUBSCRIPTION_SVC] Failed to initialize Chapa", e);
            throw new RuntimeException("Failed to initiate payment gateway", e);
        }
    }

    /**
     * Verifies by txRef (used by Chapa callback). Falls back to subscriptionId
     * lookup.
<<<<<<< HEAD
=======
     * On success:
     * 1. Marks the subscription ACTIVE with an expiry (30 days for monthly, 365 for
     * annual/default).
     * 2. Supersedes any previously ACTIVE subscriptions for the same subject so
     * only one is live.
     * 3. Records a detailed audit trail.
>>>>>>> 82aa1f0 (Initial commit)
     */
    public boolean verifyCheckout(String txRef, Long subscriptionId) {
        log.info("[SUBSCRIPTION_SVC] verifyCheckout txRef={} subId={}", txRef, subscriptionId);
        boolean isSuccess = chapaPaymentService.verifyChapaPayment(txRef);

        if (isSuccess) {
<<<<<<< HEAD
            // Try to find by txRef first (more reliable), fall back to id
=======
            // Resolve subscription — txRef is more reliable (immutable after creation)
>>>>>>> 82aa1f0 (Initial commit)
            Optional<Subscription> subOpt = repo.findByTxRef(txRef);
            Subscription s = subOpt.orElseGet(() -> repo.findById(subscriptionId)
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId)));

<<<<<<< HEAD
            s.setStatus("ACTIVE");
            s.setActivatedAt(Instant.now());
            // Grant 1-year expiry for annual plan, 30 days for monthly
            s.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            repo.save(s);
            try {
                auditService.record("subscription.activate", "system", "subId=" + s.getId() + " txRef=" + txRef);
            } catch (Exception ignored) {
            }
            log.info("[SUBSCRIPTION_SVC] Subscription {} activated for userId={}", s.getId(), s.getSubjectId());
            return true;
        }
=======
            // Supersede any existing ACTIVE subscriptions for the same subject
            List<Subscription> existing = repo.findBySubjectTypeAndSubjectIdOrderByIdDesc(
                    s.getSubjectType(), s.getSubjectId());
            for (Subscription prev : existing) {
                if (prev.getId().equals(s.getId()))
                    continue;
                if ("ACTIVE".equals(prev.getStatus())) {
                    prev.setStatus("SUPERSEDED");
                    repo.save(prev);
                    log.info("[SUBSCRIPTION_SVC] Superseded old subscription id={}", prev.getId());
                }
            }

            // Determine expiry: planId 1 = monthly (30d), everything else = annual (365d)
            long durationDays = (s.getPlanId() != null && s.getPlanId() == 1L) ? 30L : 365L;

            s.setStatus("ACTIVE");
            s.setActivatedAt(Instant.now());
            s.setExpiresAt(Instant.now().plus(durationDays, ChronoUnit.DAYS));
            repo.save(s);

            try {
                auditService.record("subscription.activate", "system",
                        String.format("subId=%d txRef=%s subject=%s/%d expiresInDays=%d",
                                s.getId(), txRef, s.getSubjectType(), s.getSubjectId(), durationDays));
            } catch (Exception ignored) {
            }

            log.info("[SUBSCRIPTION_SVC] ✅ Activated subId={} subject={}/{} durationDays={}",
                    s.getId(), s.getSubjectType(), s.getSubjectId(), durationDays);
            return true;
        }

        log.warn("[SUBSCRIPTION_SVC] ❌ Chapa verification failed for txRef={}", txRef);
>>>>>>> 82aa1f0 (Initial commit)
        return false;
    }

    public Subscription create(Subscription s) {
        s.setStatus("ACTIVE");
        var saved = repo.save(s);
        try {
            auditService.record("subscription.create", "system", "id=" + saved.getId());
        } catch (Exception ignored) {
        }
        return saved;
    }

    public List<Subscription> getByUserId(Long userId) {
        return repo.findBySubjectTypeAndSubjectIdOrderByIdDesc("USER", userId);
    }

    public List<Subscription> getByOrgId(Long orgId) {
        return repo.findBySubjectTypeAndSubjectIdOrderByIdDesc("ORG", orgId);
    }

    public record CheckoutResult(Long subscriptionId, String checkoutUrl) {
    }
}
