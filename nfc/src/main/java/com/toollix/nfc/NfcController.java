package com.toollix.nfc;

import com.toollix.nfc.service.NfcService;
import com.toollix.common.entitlement.EntitlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class NfcController {

    private final NfcService nfcService;
    private final EntitlementService entitlementService;

    public NfcController(NfcService nfcService, EntitlementService entitlementService) { this.nfcService = nfcService; this.entitlementService = entitlementService; }

    @GetMapping("/tap/{internalCode}")
    public ResponseEntity<?> tap(@PathVariable String internalCode) {
        var maybe = nfcService.findByInternalCode(internalCode);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("tapped", internalCode, "status", maybe.get().getStatus()));
    }

    @PostMapping("/nfc/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody AssignReq req, @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        if (!entitlementService.hasFeatureForProfile(req.profileId, "NFC_CARD")) {
            return ResponseEntity.status(403).body(Map.of("error", "entitlement_required"));
        }

        var maybe = nfcService.findByInternalCode(req.internalCode);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        var card = maybe.get();
        var a = nfcService.assignToProfile(card, req.profileId);
        return ResponseEntity.ok(a);
    }

    @PostMapping("/nfc/{id}/report-lost")
    public ResponseEntity<?> reportLost(@PathVariable Long id) {
        return ResponseEntity.ok(nfcService.markLost(id));
    }

    @PostMapping("/admin/nfc/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable Long id) {
        return ResponseEntity.ok(nfcService.retire(id));
    }

    public static record AssignReq(String internalCode, Long profileId) {}
}
