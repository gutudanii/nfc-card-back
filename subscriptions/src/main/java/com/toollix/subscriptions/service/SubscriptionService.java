package com.toollix.subscriptions.service;

import com.toollix.subscriptions.model.Subscription;
import com.toollix.subscriptions.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
    private final SubscriptionRepository repo;
    private final com.toollix.common.audit.AuditService auditService;
    public SubscriptionService(SubscriptionRepository repo, com.toollix.common.audit.AuditService auditService) { this.repo = repo; this.auditService = auditService; }
    public Subscription create(Subscription s) { s.setStatus("ACTIVE"); var saved = repo.save(s); try { auditService.record("subscription.create", "system", "id="+saved.getId()); } catch (Exception ignored) {} return saved; }
}
