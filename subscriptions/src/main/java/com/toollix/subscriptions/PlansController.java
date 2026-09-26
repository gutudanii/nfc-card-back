package com.toollix.subscriptions;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public + Admin endpoints for the Plans catalog and NFC card types.
 *
 * GET /plans — public: returns all active plans
 * GET /plans/nfc-types — public: returns all active NFC card types
 * PUT /plans/{id} — admin only: update name/price/features
 * PUT /plans/nfc-types/{id} — admin only: update NFC type price
 */
@RestController
@RequestMapping("/plans")
public class PlansController {

    private final JdbcTemplate jdbc;

    public PlansController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC: List all active plans
    // ─────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listPlans() {
        List<Map<String, Object>> plans = jdbc.queryForList(
                "SELECT id, code, name, description, billing_type, billing_cycle, target_type, " +
                        "min_seats, max_seats, min_months, price_cents, features, sort_order " +
                        "FROM plans WHERE active = TRUE ORDER BY sort_order ASC");
        return ResponseEntity.ok(plans);
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC: List all active NFC card types
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/nfc-types")
    public ResponseEntity<?> listNfcTypes() {
        List<Map<String, Object>> types = jdbc.queryForList(
                "SELECT id, code, name, description, price_cents FROM nfc_card_types WHERE active = TRUE ORDER BY price_cents ASC");
        return ResponseEntity.ok(types);
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: Update a plan (price, name, description, features, active)
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable("id") Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String principal) {
        // Simple admin guard — only admins can call this (secured via SecurityConfig)
        if (body.containsKey("name"))
            jdbc.update("UPDATE plans SET name = ? WHERE id = ?", body.get("name"), id);
        if (body.containsKey("description"))
            jdbc.update("UPDATE plans SET description = ? WHERE id = ?", body.get("description"), id);
        if (body.containsKey("price_cents"))
            jdbc.update("UPDATE plans SET price_cents = ? WHERE id = ?",
                    Long.parseLong(body.get("price_cents").toString()), id);
        if (body.containsKey("active"))
            jdbc.update("UPDATE plans SET active = ? WHERE id = ?", Boolean.parseBoolean(body.get("active").toString()),
                    id);
        if (body.containsKey("features"))
            jdbc.update("UPDATE plans SET features = ?::jsonb WHERE id = ?", body.get("features").toString(), id);

        Map<String, Object> updated = jdbc.queryForMap("SELECT * FROM plans WHERE id = ?", id);
        return ResponseEntity.ok(updated);
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: Update an NFC card type price or name
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/nfc-types/{id}")
    public ResponseEntity<?> updateNfcType(@PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        if (body.containsKey("name"))
            jdbc.update("UPDATE nfc_card_types SET name = ? WHERE id = ?", body.get("name"), id);
        if (body.containsKey("price_cents"))
            jdbc.update("UPDATE nfc_card_types SET price_cents = ? WHERE id = ?",
                    Long.parseLong(body.get("price_cents").toString()), id);
        if (body.containsKey("description"))
            jdbc.update("UPDATE nfc_card_types SET description = ? WHERE id = ?", body.get("description"), id);
        if (body.containsKey("active"))
            jdbc.update("UPDATE nfc_card_types SET active = ? WHERE id = ?",
                    Boolean.parseBoolean(body.get("active").toString()), id);

        Map<String, Object> updated = jdbc.queryForMap("SELECT * FROM nfc_card_types WHERE id = ?", id);
        return ResponseEntity.ok(updated);
    }
}
