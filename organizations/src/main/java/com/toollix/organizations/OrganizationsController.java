package com.toollix.organizations;

import com.toollix.common.storage.FileStorageService;
import com.toollix.organizations.model.Organization;
import com.toollix.organizations.model.OrganizationMember;
import com.toollix.organizations.model.Team;
import com.toollix.organizations.repo.OrganizationMemberRepository;
import com.toollix.organizations.repo.OrganizationRepository;
import com.toollix.organizations.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Organization management endpoints.
 *
 * Role enforcement:
 * POST /orgs — any authenticated user (creates org, becomes OWNER)
 * GET /orgs/my — authenticated user (returns their orgs)
 * GET /orgs/{id} — org member only
 * PUT /orgs/{id} — ADMIN or OWNER only (update name/slug/tagline/brandColor)
 * POST /orgs/{id}/logo — ADMIN or OWNER only (upload logo)
 * POST /orgs/{id}/members — ADMIN or OWNER only
 * GET /orgs/{id}/members — any member
 * PUT /orgs/{id}/members/{userId}/role — OWNER only (change role)
 * DELETE /orgs/{id}/members/{userId} — ADMIN or OWNER only (remove member)
 * POST /orgs/{id}/teams — ADMIN or OWNER only
 */
@RestController
@RequestMapping("/orgs")
public class OrganizationsController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationsController.class);

    private final OrganizationService service;
    private final OrganizationRepository repo;
    private final OrganizationMemberRepository memberRepo;
    private final FileStorageService storageService;
    private final com.toollix.common.mail.EmailService emailService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public OrganizationsController(OrganizationService service,
            OrganizationRepository repo,
            OrganizationMemberRepository memberRepo,
            FileStorageService storageService,
            com.toollix.common.mail.EmailService emailService,
            org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.service = service;
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.storageService = storageService;
        this.emailService = emailService;
        this.jdbc = jdbc;
    }

    // ─── Create org ───────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateOrgRequest req,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        log.info("[ORGS_CTRL] POST /orgs — name='{}' callerId={}", req.name(), callerId);

        Organization o = new Organization();
        o.setName(req.name());
        o.setSlug(req.slug());
        if (req.tagline() != null)
            o.setTagline(req.tagline());
        Organization saved = service.create(o, callerId);

        log.info("[ORGS_CTRL] Org created — id={} slug={} ownerId={}", saved.getId(), saved.getSlug(), callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─── My orgs ──────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<?> myOrgs(@AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        var memberships = memberRepo.findByUserIdAndStatus(callerId, "ACTIVE");

        var results = memberships.stream().map(m -> {
            var org = repo.findById(m.getOrgId()).orElse(null);
            if (org == null)
                return null;
            return Map.of(
                    "org", org,
                    "role", m.getRole());
        }).filter(java.util.Objects::nonNull).toList();

        return ResponseEntity.ok(results);
    }

    // ─── Public org lookup by slug (no auth) ─────────────────────────────────

    @GetMapping("/public/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable("slug") String slug) {
        return repo.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Get org by ID ────────────────────────────────────────────────────────

    @GetMapping("/{orgId}")
    public ResponseEntity<?> getById(@PathVariable("orgId") Long orgId,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        // Ensure caller is a member
        boolean isMember = memberRepo.findByOrgIdAndUserIdAndStatus(orgId, callerId, "ACTIVE").isPresent();
        if (!isMember)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not a member"));

        return repo.findById(orgId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Update org (branding / name) ─────────────────────────────────────────

    @PutMapping("/{orgId}")
    public ResponseEntity<?> update(@PathVariable("orgId") Long orgId,
            @AuthenticationPrincipal String principal,
            @RequestBody UpdateOrgRequest req) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        return repo.findById(orgId).map(o -> {
            if (req.name() != null)
                o.setName(req.name());
            if (req.tagline() != null)
                o.setTagline(req.tagline());
            if (req.brandColor() != null)
                o.setBrandColor(req.brandColor());
            var saved = repo.save(o);
            log.info("[ORGS_CTRL] PUT /orgs/{} updated by userId={}", orgId, callerId);
            return ResponseEntity.ok(saved);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Upload org logo ──────────────────────────────────────────────────────

    @PostMapping("/{orgId}/logo")
    public ResponseEntity<?> uploadLogo(@PathVariable("orgId") Long orgId,
            @AuthenticationPrincipal String principal,
            @RequestParam("file") MultipartFile file) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        return repo.findById(orgId).map(o -> {
            String url = storageService.storeFile(file);
            o.setLogoUrl(url);
            repo.save(o);
            log.info("[ORGS_CTRL] Logo uploaded orgId={} url={}", orgId, url);
            return ResponseEntity.ok(Map.of("logoUrl", url));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    @GetMapping("/{orgId}/members")
    public ResponseEntity<?> members(@PathVariable("orgId") Long orgId,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        boolean isMember = memberRepo.findByOrgIdAndUserIdAndStatus(orgId, callerId, "ACTIVE").isPresent();
        if (!isMember)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not a member"));

        var list = service.listMembers(orgId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable("orgId") Long orgId,
            @Valid @RequestBody AddMemberRequest req,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        log.info("[ORGS_CTRL] POST /orgs/{}/members — adding userId={} role={} by callerId={}", orgId, req.userId(),
                req.role(), callerId);
        OrganizationMember member = service.addMember(orgId, req.userId(), req.role(), req.department());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/{orgId}/invite")
    public ResponseEntity<?> inviteMember(
            @PathVariable("orgId") Long orgId,
            @Valid @RequestBody InviteRequest req,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        var org = repo.findById(orgId).orElseThrow();

        // 1. Check if user exists
        Long existingUserId = null;
        try {
            existingUserId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, req.email());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // User doesn't exist
        }

        if (existingUserId != null) {
            // Already exists -> just add them to the org
            try {
                service.addMember(orgId, existingUserId, req.role(), null);
            } catch (Exception e) {
                // Ignore if already a member
            }
        } else {
            // Log that user does not exist (we don't block invitation, just notify)
            // They can create an account and join later via invite flow (or we can
            // auto-create the shell account).
            log.info("[ORGS_CTRL] Invite sent to non-existent user email={}", req.email());
        }

        // Use the proper callback URL so it hits the specific organization page.
        String inviteLink = "http://localhost:3000/login?callbackUrl=/org/" + orgId;
        emailService.sendOrganizationInvitation(req.email(), org.getName(), req.role() != null ? req.role() : "MEMBER",
                inviteLink);

        log.info("[ORGS_CTRL] POST /orgs/{}/invite — emailed {} role={} by callerId={}", orgId, req.email(), req.role(),
                callerId);
        return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("orgId") Long orgId,
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        memberRepo.findByOrgIdAndUserIdAndStatus(orgId, userId, "ACTIVE").ifPresent(m -> {
            m.setStatus("REMOVED");
            memberRepo.save(m);
        });
        log.info("[ORGS_CTRL] Member userId={} removed from orgId={} by callerId={}", userId, orgId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orgId}/members/{userId}/role")
    public ResponseEntity<?> updateRole(@PathVariable("orgId") Long orgId,
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal String principal,
            @RequestBody RoleRequest req) {
        Long callerId = Long.parseLong(principal);
        if (!service.isOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires OWNER role"));

        return memberRepo.findByOrgIdAndUserIdAndStatus(orgId, userId, "ACTIVE").map(m -> {
            m.setRole(req.role());
            memberRepo.save(m);
            log.info("[ORGS_CTRL] Role updated userId={} orgId={} role={}", userId, orgId, req.role());
            return ResponseEntity.ok(m);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Teams ────────────────────────────────────────────────────────────────

    @PostMapping("/{orgId}/teams")
    public ResponseEntity<?> createTeam(
            @PathVariable("orgId") Long orgId,
            @Valid @RequestBody CreateTeamRequest req,
            @AuthenticationPrincipal String principal) {
        Long callerId = Long.parseLong(principal);
        if (!service.isAdminOrOwner(orgId, callerId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Requires ADMIN or OWNER role"));

        log.info("[ORGS_CTRL] POST /orgs/{}/teams — name='{}' by callerId={}", orgId, req.name(), callerId);
        Team team = service.createTeam(orgId, req.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    // ─── Request records ──────────────────────────────────────────────────────

    public record CreateOrgRequest(@NotBlank String name, @NotBlank String slug, String tagline) {
    }

    public record UpdateOrgRequest(String name, String tagline, String brandColor) {
    }

    public record AddMemberRequest(@NotNull Long userId, String role, String department) {
    }

    public record InviteRequest(@NotBlank String email, String role) {
    }

    public record RoleRequest(@NotBlank String role) {
    }

    public record CreateTeamRequest(@NotBlank String name) {
    }
}
