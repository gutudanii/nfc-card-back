package com.toollix.admin;

import com.toollix.nfc.repo.NfcCardRepository;
import com.toollix.orders.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
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
=======
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Internal platform admin endpoints.
 * ALL routes require isPlatformAdmin() — SUPER_ADMIN, SUPPORT_ADMIN, or
 * OPERATIONS_ADMIN.
>>>>>>> 82aa1f0 (Initial commit)
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("@accessGuard.isPlatformAdmin(authentication)")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final NfcCardRepository nfcRepo;
    private final OrderRepository orderRepo;
<<<<<<< HEAD

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
=======
    private final JdbcTemplate jdbc;

    public AdminController(NfcCardRepository nfcRepo, OrderRepository orderRepo, JdbcTemplate jdbc) {
        this.nfcRepo = nfcRepo;
        this.orderRepo = orderRepo;
        this.jdbc = jdbc;
    }

    // ─── Platform Stats ────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal String principal) {
        long totalUsers = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        long activeUsers = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'", Long.class);
        long totalOrgs = jdbc.queryForObject("SELECT COUNT(*) FROM organizations", Long.class);
        long totalCards = jdbc.queryForObject("SELECT COUNT(*) FROM nfc_cards", Long.class);
        long assignedCards = jdbc.queryForObject("SELECT COUNT(*) FROM nfc_cards WHERE status = 'ACTIVE'", Long.class);
        long totalOrders = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        long totalSubs = jdbc.queryForObject("SELECT COUNT(*) FROM subscriptions", Long.class);
        long activeSubs = jdbc.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE status = 'ACTIVE'", Long.class);
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "totalOrgs", totalOrgs,
                "totalCards", totalCards,
                "assignedCards", assignedCards,
                "totalOrders", totalOrders,
                "totalSubs", totalSubs,
                "activeSubs", activeSubs));
    }

    // ─── NFC Inventory ─────────────────────────────────────────────────────────

    @GetMapping("/nfc/inventory")
    public ResponseEntity<?> nfcInventory(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/nfc/inventory — callerId={}", principal);
        return ResponseEntity.ok(nfcRepo.findAll());
    }

    @PatchMapping("/nfc/inventory/{id}/status")
    public ResponseEntity<?> updateCardStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String status = body.get("status");
        log.info("[ADMIN_CTRL] PATCH /admin/nfc/inventory/{}/status={} — callerId={}", id, status, principal);
        int updated = jdbc.update("UPDATE nfc_cards SET status = ? WHERE id = ?", status, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Card status updated"));
    }

    @DeleteMapping("/nfc/inventory/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable("id") Long id, @AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] DELETE /admin/nfc/inventory/{} — callerId={}", id, principal);
        int deleted = jdbc.update("DELETE FROM nfc_cards WHERE id = ?", id);
        if (deleted == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Card deleted"));
    }

    // ─── Orders ────────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/orders — callerId={}", principal);
        return ResponseEntity.ok(orderRepo.findAll());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String status = body.get("status");
        log.info("[ADMIN_CTRL] PATCH /admin/orders/{}/status={} — callerId={}", id, status, principal);
        int updated = jdbc.update("UPDATE orders SET status = ? WHERE id = ?", status, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Order status updated"));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") Long id, @AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] DELETE /admin/orders/{} — callerId={}", id, principal);
        int deleted = jdbc.update("DELETE FROM orders WHERE id = ?", id);
        if (deleted == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Order deleted"));
    }

    // ─── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> users(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/users — callerId={}", principal);
        return ResponseEntity.ok(jdbc.queryForList(
                "SELECT id, email, phone, status, platform_role as \"platformRole\", created_at as \"createdAt\" FROM users ORDER BY id DESC"));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String role = body.get("platformRole");
        log.info("[ADMIN_CTRL] PATCH /admin/users/{}/role={} — callerId={}", id, role, principal);
        // null means strip admin rights
        int updated = jdbc.update("UPDATE users SET platform_role = ? WHERE id = ?",
                (role == null || role.isBlank()) ? null : role, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "User role updated"));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String status = body.get("status");
        log.info("[ADMIN_CTRL] PATCH /admin/users/{}/status={} — callerId={}", id, status, principal);
        int updated = jdbc.update("UPDATE users SET status = ? WHERE id = ?", status, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "User status updated"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id, @AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] DELETE /admin/users/{} — callerId={}", id, principal);
        int deleted = jdbc.update("DELETE FROM users WHERE id = ?", id);
        if (deleted == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // ─── Subscriptions ─────────────────────────────────────────────────────────

    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> subscriptions(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/subscriptions — callerId={}", principal);
        return ResponseEntity.ok(jdbc.queryForList(
                "SELECT s.*, p.name as \"planName\", p.target_type as \"planTargetType\" " +
                        "FROM subscriptions s LEFT JOIN plans p ON s.plan_id = p.id ORDER BY s.id DESC"));
    }

    @PatchMapping("/subscriptions/{id}/status")
    public ResponseEntity<?> updateSubStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String status = body.get("status");
        log.info("[ADMIN_CTRL] PATCH /admin/subscriptions/{}/status={} — callerId={}", id, status, principal);
        int updated = jdbc.update("UPDATE subscriptions SET status = ? WHERE id = ?", status, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Subscription status updated"));
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<?> deleteSub(@PathVariable("id") Long id, @AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] DELETE /admin/subscriptions/{} — callerId={}", id, principal);
        int deleted = jdbc.update("DELETE FROM subscriptions WHERE id = ?", id);
        if (deleted == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Subscription deleted"));
    }

    // ─── Organizations ─────────────────────────────────────────────────────────

    @GetMapping("/organizations")
    public ResponseEntity<List<Map<String, Object>>> organizations(@AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] GET /admin/organizations — callerId={}", principal);
        return ResponseEntity.ok(jdbc.queryForList(
                "SELECT o.*, (SELECT COUNT(*) FROM organization_members m WHERE m.org_id = o.id) as \"memberCount\" " +
                        "FROM organizations o ORDER BY o.id DESC"));
    }

    @PatchMapping("/organizations/{id}/status")
    public ResponseEntity<?> updateOrgStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String principal) {
        String status = body.get("status");
        log.info("[ADMIN_CTRL] PATCH /admin/organizations/{}/status={} — callerId={}", id, status, principal);
        int updated = jdbc.update("UPDATE organizations SET status = ? WHERE id = ?", status, id);
        if (updated == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Organization status updated"));
    }

    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<?> deleteOrg(@PathVariable("id") Long id, @AuthenticationPrincipal String principal) {
        log.info("[ADMIN_CTRL] DELETE /admin/organizations/{} — callerId={}", id, principal);
        int deleted = jdbc.update("DELETE FROM organizations WHERE id = ?", id);
        if (deleted == 0)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Organization deleted"));
>>>>>>> 82aa1f0 (Initial commit)
    }
}
