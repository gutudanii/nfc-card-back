package com.toollix.admin;

import com.toollix.nfc.repo.NfcCardRepository;
import com.toollix.orders.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Internal platform admin endpoints.
 *
 * ALL routes here require isPlatformAdmin() — meaning the caller must hold
 * a platform-level role (SUPER_ADMIN, SUPPORT_ADMIN, OPERATIONS_ADMIN)
 * stored on the users table. Org roles alone are NOT sufficient.
 *
 * Platform roles are assigned by ops directly in the database — never via API.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("@accessGuard.isPlatformAdmin(authentication)")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final NfcCardRepository nfcRepo;
    private final OrderRepository orderRepo;

    public AdminController(NfcCardRepository nfcRepo, OrderRepository orderRepo) {
        this.nfcRepo = nfcRepo;
        this.orderRepo = orderRepo;
    }

    @GetMapping("/nfc/inventory")
    public ResponseEntity<?> nfcInventory(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/nfc/inventory — callerId={}", principal);
        var inventory = nfcRepo.findAll();
        log.info("[ADMIN_CTRL] nfc/inventory — returned {} cards", inventory.size());
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/orders — callerId={}", principal);
        var orders = orderRepo.findAll();
        log.info("[ADMIN_CTRL] orders — returned {} orders", orders.size());
        return ResponseEntity.ok(orders);
    }
}
