package com.toollix.subscriptions;

import com.toollix.subscriptions.model.Subscription;
import com.toollix.subscriptions.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionsController {
    private final SubscriptionService service;
    public SubscriptionsController(SubscriptionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Subscription s) { return ResponseEntity.ok(service.create(s)); }
}
