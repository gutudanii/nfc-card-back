package com.toollix.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.toollix.analytics.repo.AnalyticsEventRepository;
import com.toollix.analytics.model.AnalyticsEvent;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsEventRepository eventRepo;
    private final JdbcTemplate jdbc;

    public AnalyticsController(AnalyticsEventRepository eventRepo, JdbcTemplate jdbc) {
        this.eventRepo = eventRepo;
        this.jdbc = jdbc;
    }

    /** Look up profile_id from the numeric userId stored in the JWT principal. */
    private Long getProfileId(String userIdStr) {
        if (userIdStr == null)
            return null;
        try {
            Long userId = Long.parseLong(userIdStr);
            return jdbc.queryForObject(
                    "SELECT id FROM profiles WHERE user_id = ?", Long.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("[ANALYTICS] getProfileId failed for userIdStr={}: {}", userIdStr, e.getMessage());
            return null;
        }
    }

    /** Look up profile_id from a username slug (used for public event tracking). */
    private Long getProfileIdByUsername(String username) {
        if (username == null)
            return null;
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM profiles WHERE username = ?", Long.class, username);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("[ANALYTICS] getProfileIdByUsername failed for username={}: {}", username, e.getMessage());
            return null;
        }
    }

    // ── Public event tracking ──────────────────────────────────────────────────

    /**
     * POST /p/{username}/analytics/event
     * Permitted to anonymous callers via ""/p/**"" in SecurityConfig.
     * Optionally checks JWT (if present) to skip owner self-views without error.
     */
    @PostMapping("/p/{username}/analytics/event")
    public ResponseEntity<?> trackPublicEvent(
            @PathVariable("username") String username,
            @RequestBody(required = false) Map<String, String> payload,
            @AuthenticationPrincipal String principal) {

        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing payload"));
        }

        String eventType = payload.get("eventType");
        if (eventType == null || eventType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventType is required"));
        }

        Long profileId = getProfileIdByUsername(username);
        if (profileId == null) {
            // Profile not found — silently 200 so the public page never shows errors
            log.debug("[ANALYTICS] No profile found for username={}, skipping", username);
            return ResponseEntity.ok(Map.of("status", "profile_not_found"));
        }

        // Skip tracking if the authenticated user IS the profile owner
        if (principal != null) {
            Long ownerProfileId = getProfileId(principal);
            if (profileId.equals(ownerProfileId)) {
                log.debug("[ANALYTICS] Skipping owner self-view for username={}", username);
                return ResponseEntity.ok(Map.of("status", "skipped_owner"));
            }
        }

        try {
            AnalyticsEvent evt = new AnalyticsEvent();
            evt.setProfileId(profileId);
            evt.setEventType(eventType);
<<<<<<< HEAD
            eventRepo.save(evt);
            log.debug("[ANALYTICS] Tracked {} for profileId={}", eventType, profileId);
=======

            // Build meta JSON from extra payload fields (url, title, etc.)
            // We strip eventType from the extra fields and serialize the rest as JSONB.
            Map<String, String> metaFields = new java.util.LinkedHashMap<>(payload);
            metaFields.remove("eventType");
            if (!metaFields.isEmpty()) {
                // Simple manual JSON serialize to avoid pulling in Jackson explicitly
                StringBuilder sb = new StringBuilder("{");
                metaFields.forEach((k, v) -> sb
                        .append("\"").append(k.replace("\"", "\\\"")).append("\":")
                        .append("\"").append(v == null ? "" : v.replace("\"", "\\\"")).append("\","));
                sb.deleteCharAt(sb.length() - 1); // remove trailing comma
                sb.append("}");
                evt.setMeta(sb.toString());
            }

            eventRepo.save(evt);
            log.debug("[ANALYTICS] Tracked {} for profileId={} meta={}", eventType, profileId, evt.getMeta());
>>>>>>> 82aa1f0 (Initial commit)
            return ResponseEntity.ok(Map.of("status", "tracked"));
        } catch (Exception e) {
            log.error("[ANALYTICS] Failed to save event for username={}: {}", username, e.getMessage());
            // Return 200 — analytics should NEVER break the visitor's UX
            return ResponseEntity.ok(Map.of("status", "error"));
        }
    }

    // ── Authenticated summary ──────────────────────────────────────────────────

    @GetMapping("/me/analytics/summary")
    public ResponseEntity<?> summary(
            @AuthenticationPrincipal String principal,
            @RequestParam(name = "days", defaultValue = "30") int days) {

        Long profileId = getProfileId(principal);
        if (profileId == null) {
            return ResponseEntity.ok(Map.of(
                    "profileViews", 0, "nfcTaps", 0, "linkClicks", 0, "contactSaves", 0));
        }

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        long profileViews = eventRepo.countEventsSince(profileId, "PROFILE_VIEW", since);
        long nfcTaps = eventRepo.countEventsSince(profileId, "NFC_TAP", since);
        long linkClicks = eventRepo.countEventsSince(profileId, "LINK_CLICK", since);
        long contactSaves = eventRepo.countEventsSince(profileId, "CONTACT_SAVE", since);

        return ResponseEntity.ok(Map.of(
                "profileViews", profileViews,
                "nfcTaps", nfcTaps,
                "linkClicks", linkClicks,
                "contactSaves", contactSaves));
    }

    // ── Authenticated time-series ──────────────────────────────────────────────

    @GetMapping("/me/analytics/timeseries")
    public ResponseEntity<?> timeseries(
            @AuthenticationPrincipal String principal,
            @RequestParam(name = "days", defaultValue = "30") int days) {

        Long profileId = getProfileId(principal);
        if (profileId == null) {
            return ResponseEntity.ok(List.of());
        }

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<AnalyticsEvent> events = eventRepo.findEventsSince(profileId, since);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

        Map<String, Map<String, Long>> grouped = events.stream().collect(
                Collectors.groupingBy(
                        e -> dtf.format(e.getOccurredAt()),
                        Collectors.groupingBy(
                                AnalyticsEvent::getEventType,
                                Collectors.counting())));

        var result = grouped.entrySet().stream()
                .map(entry -> {
                    String date = entry.getKey();
                    var counts = entry.getValue();
                    return Map.of(
                            "date", date,
                            "profileViews", counts.getOrDefault("PROFILE_VIEW", 0L),
                            "nfcTaps", counts.getOrDefault("NFC_TAP", 0L),
                            "linkClicks", counts.getOrDefault("LINK_CLICK", 0L),
                            "contactSaves", counts.getOrDefault("CONTACT_SAVE", 0L));
                })
                .sorted((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
<<<<<<< HEAD
=======

    // ── Top clicked links ─────────────────────────────────────────────────────

    /**
     * GET /me/analytics/top-links?days=30&limit=10
     *
     * Extracts link URL and title from the JSONB meta column of LINK_CLICK events,
     * groups by (url, title), and returns them ranked by click count descending.
     *
     * meta JSON structure expected: { "url": "https://...", "title": "LinkedIn" }
     */
    @GetMapping("/me/analytics/top-links")
    public ResponseEntity<?> topLinks(
            @AuthenticationPrincipal String principal,
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        Long profileId = getProfileId(principal);
        if (profileId == null) {
            return ResponseEntity.ok(List.of());
        }

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        // PostgreSQL JSONB extraction. Falls back gracefully if meta is null or not
        // parseable.
        String sql = """
                SELECT
                    COALESCE(meta::json->>'url',   'unknown') AS url,
                    COALESCE(meta::json->>'title', 'Link')    AS title,
                    COUNT(*)                                   AS clicks
                FROM analytics_events
                WHERE profile_id = ?
                  AND event_type = 'LINK_CLICK'
                  AND occurred_at >= ?
                  AND meta IS NOT NULL
                GROUP BY url, title
                ORDER BY clicks DESC
                LIMIT ?
                """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, profileId, since, limit);

        log.info("[ANALYTICS] GET /me/analytics/top-links — profileId={} days={} rows={}", profileId, days,
                rows.size());
        return ResponseEntity.ok(rows);
    }
>>>>>>> 82aa1f0 (Initial commit)
}
