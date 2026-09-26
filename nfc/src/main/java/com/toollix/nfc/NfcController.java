package com.toollix.nfc;

import com.toollix.common.entitlement.EntitlementService;
import com.toollix.nfc.model.NfcAssignment;
import com.toollix.nfc.repo.NfcAssignmentRepository;
import com.toollix.nfc.service.NfcService;
import com.toollix.profiles.repo.ProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class NfcController {

    private static final Logger log = LoggerFactory.getLogger(NfcController.class);

    private final NfcService nfcService;
    private final EntitlementService entitlementService;
    private final NfcAssignmentRepository assignmentRepo;
    private final ProfileRepository profileRepo;

    public NfcController(NfcService nfcService, EntitlementService entitlementService,
            NfcAssignmentRepository assignmentRepo, ProfileRepository profileRepo) {
        this.nfcService = nfcService;
        this.entitlementService = entitlementService;
        this.assignmentRepo = assignmentRepo;
        this.profileRepo = profileRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // My NFC Cards — returns all cards assigned to the current user's profile
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/nfc/my-cards")
    public ResponseEntity<?> myCards(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        log.info("[NFC_CTRL] GET /nfc/my-cards — userId={}", userId);

        var profileOpt = profileRepo.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            log.debug("[NFC_CTRL] my-cards — no profile for userId={}, returning empty list", userId);
            return ResponseEntity.ok(List.of());
        }

        Long profileId = profileOpt.get().getId();
        List<NfcAssignment> assignments = assignmentRepo.findByProfileIdOrderByAssignedAtDesc(profileId);

        // Build rich response: assignment metadata + card details
        var result = assignments.stream()
                .filter(a -> a.getUnassignedAt() == null) // only currently active assignments
                .map(a -> Map.of(
                        "assignmentId", a.getId(),
                        "assignedAt", a.getAssignedAt().toString(),
                        "card", Map.of(
                                "id", a.getNfcCard().getId(),
                                "internalCode", a.getNfcCard().getInternalCode(),
                                "status", a.getNfcCard().getStatus())))
                .toList();

        log.info("[NFC_CTRL] my-cards — userId={} profileId={} cards={}", userId, profileId, result.size());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public NFC tap
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/tap/{internalCode}")
    public ResponseEntity<?> tap(@PathVariable("internalCode") String internalCode) {
        log.info("[NFC_CTRL] GET /tap/{} — incoming NFC tap", internalCode);
        var maybe = nfcService.findByInternalCode(internalCode);
        if (maybe.isEmpty()) {
            log.warn("[NFC_CTRL] tap — no card found for internalCode={}", internalCode);
            return ResponseEntity.notFound().build();
        }
        var card = maybe.get();
        nfcService.recordTapAsync(card);
        return ResponseEntity.ok(Map.of("tapped", internalCode, "status", card.getStatus()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Assign
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/nfc/assign")
    @PreAuthorize("@accessGuard.canEditProfile(#req.profileId, authentication)")
    public ResponseEntity<?> assign(@Valid @RequestBody AssignReq req,
            @AuthenticationPrincipal String principal) {
        log.info("[NFC_CTRL] POST /nfc/assign — internalCode={} profileId={} by userId={}", req.internalCode(),
                req.profileId(), principal);
        if (!entitlementService.hasFeatureForProfile(req.profileId(), "NFC_CARD")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "entitlement_required", "feature", "NFC_CARD"));
        }
        var maybe = nfcService.findByInternalCode(req.internalCode());
        if (maybe.isEmpty())
            return ResponseEntity.notFound().build();
        var assignment = nfcService.assignToProfile(maybe.get(), req.profileId());
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Report Lost
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/nfc/{id}/report-lost")
    public ResponseEntity<?> reportLost(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal) {
        log.info("[NFC_CTRL] POST /nfc/{}/report-lost — userId={}", id, principal);
        var card = nfcService.markLost(id);
        return ResponseEntity.ok(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retire (admin only)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/admin/nfc/{id}/retire")
    @PreAuthorize("@accessGuard.isPlatformAdmin(authentication)")
    public ResponseEntity<?> retire(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal) {
        log.info("[NFC_CTRL] POST /admin/nfc/{}/retire — userId={}", id, principal);
        var card = nfcService.retire(id);
        return ResponseEntity.ok(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Request records
    // ─────────────────────────────────────────────────────────────────────────

    public record AssignReq(@NotBlank String internalCode, @NotNull Long profileId) {
    }
}
