package com.toollix.profiles;

import com.toollix.common.storage.FileStorageService;
import com.toollix.profiles.model.ContactLink;
import com.toollix.profiles.model.Profile;
import com.toollix.profiles.repo.ContactLinkRepository;
import com.toollix.profiles.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Handles all profile-related endpoints:
 * - Public profile view: GET /p/{username}
 * - Authenticated profile management: GET/PUT /me/profile, POST
 * /me/profile/avatar
 * - Contact links CRUD: GET/POST /me/links, DELETE /me/links/{id}, PUT
 * /me/links/{id}/visibility
 */
@RestController
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final FileStorageService storageService;
    private final ContactLinkRepository linkRepo;
    private final com.toollix.profiles.repo.PortfolioItemRepository portfolioItemRepo;

    public ProfileController(ProfileService profileService,
            FileStorageService storageService,
            ContactLinkRepository linkRepo,
            com.toollix.profiles.repo.PortfolioItemRepository portfolioItemRepo) {
        this.profileService = profileService;
        this.storageService = storageService;
        this.linkRepo = linkRepo;
        this.portfolioItemRepo = portfolioItemRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Profile
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/p/{username}")
    public ResponseEntity<?> publicProfile(@PathVariable("username") String username) {
        log.debug("[PROFILE_CTRL] GET /p/{}", username);
        return profileService.findByUsername(username)
                .map(p -> {
                    var links = linkRepo.findByProfileIdOrderByDisplayOrderAsc(p.getId())
                            .stream().filter(ContactLink::isVisible).toList();
                    // Only include portfolio when in PROFESSIONAL mode
                    java.util.List<?> portfolio = java.util.List.of();
                    if ("PROFESSIONAL".equals(p.getMode())) {
                        portfolio = portfolioItemRepo.findByProfileIdOrderByDisplayOrderAsc(p.getId());
                    }
                    var body = new java.util.LinkedHashMap<String, Object>();
                    body.put("id", p.getId());
                    body.put("username", p.getUsername());
                    body.put("displayName", p.getDisplayName() != null ? p.getDisplayName() : p.getUsername());
                    body.put("avatarUrl", p.getAvatarUrl() != null ? p.getAvatarUrl() : "");
                    body.put("title", p.getTitle() != null ? p.getTitle() : "");
                    body.put("bio", p.getBio() != null ? p.getBio() : "");
                    body.put("location", p.getLocation() != null ? p.getLocation() : "");
                    body.put("templateId", p.getTemplateId() != null ? p.getTemplateId() : "executive");
                    body.put("mode", p.getMode());
                    body.put("contactLinks", links);
                    body.put("portfolioItems", portfolio);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authenticated — My Profile
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        return profileService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String principal,
            @RequestBody ProfileUpdate req) {
        Long userId = Long.parseLong(principal);
        var maybe = profileService.findByUserId(userId);
        Profile p = maybe.orElseGet(() -> {
            Profile newProfile = new Profile();
            newProfile.setUserId(userId);
            // Default names if missing
            newProfile.setUsername(
                    req.username() != null && !req.username().isBlank() ? req.username() : "user" + userId);
            newProfile.setDisplayName(req.displayName() != null ? req.displayName() : "New User");
            return newProfile;
        });

        if (req.displayName() != null)
            p.setDisplayName(req.displayName());
        if (req.username() != null && !req.username().isBlank())
            p.setUsername(req.username());
        if (req.title() != null)
            p.setTitle(req.title());
        if (req.bio() != null)
            p.setBio(req.bio());
        if (req.location() != null)
            p.setLocation(req.location());
        if (req.templateId() != null)
            p.setTemplateId(req.templateId());
        if (req.mode() != null)
            p.setMode(req.mode());

        p = profileService.update(p);

        log.info("[PROFILE_CTRL] PUT /me/profile — userId={} username={}", userId, p.getUsername());
        return ResponseEntity.ok(p);
    }

    @PostMapping("/me/profile/avatar")
    public ResponseEntity<?> uploadAvatar(@AuthenticationPrincipal String principal,
            @RequestParam("file") MultipartFile file) {
        Long userId = Long.parseLong(principal);
        String avatarUrl = storageService.storeFile(file);
        var p = profileService.findByUserId(userId)
                .orElseGet(() -> profileService.createForUser(userId, "user" + userId, "New User"));
        p.setAvatarUrl(avatarUrl);
        profileService.update(p);
        log.info("[PROFILE_CTRL] POST /me/profile/avatar — userId={} url={}", userId, avatarUrl);
        return ResponseEntity.ok(Map.of("message", "Avatar updated", "avatarUrl", avatarUrl));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contact Links CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/me/links")
    public ResponseEntity<?> getLinks(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        var profile = profileService.findByUserId(userId);
        if (profile.isEmpty())
            return ResponseEntity.ok(List.of());
        var links = linkRepo.findByProfileIdOrderByDisplayOrderAsc(profile.get().getId());
        log.debug("[PROFILE_CTRL] GET /me/links — userId={} count={}", userId, links.size());
        return ResponseEntity.ok(links);
    }

    @PostMapping("/me/links")
    public ResponseEntity<?> addLink(@AuthenticationPrincipal String principal,
            @Valid @RequestBody LinkRequest req) {
        Long userId = Long.parseLong(principal);
        var p = profileService.findByUserId(userId)
                .orElseGet(() -> profileService.createForUser(userId, "user" + userId, "New User"));

        // Set display_order to current count (append to end)
        int order = linkRepo.findByProfileIdOrderByDisplayOrderAsc(p.getId()).size();

        var link = new ContactLink();
        link.setProfileId(p.getId());
        link.setType(req.type());
        link.setTitle(req.title() != null ? req.title() : req.type());
        link.setUrl(req.url());
        link.setDisplayOrder(order);
        link.setVisible(true);
        var saved = linkRepo.save(link);
        log.info("[PROFILE_CTRL] POST /me/links — userId={} type={}", userId, req.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/me/links/{id}")
    public ResponseEntity<?> deleteLink(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        return linkRepo.findById(id)
                .filter(l -> {
                    var profile = profileService.findByUserId(userId);
                    return profile.isPresent() && l.getProfileId().equals(profile.get().getId());
                })
                .map(l -> {
                    linkRepo.delete(l);
                    log.info("[PROFILE_CTRL] DELETE /me/links/{} — userId={}", id, userId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me/links/{id}/visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal,
            @RequestBody VisibilityRequest req) {
        Long userId = Long.parseLong(principal);
        return linkRepo.findById(id)
                .filter(l -> {
                    var profile = profileService.findByUserId(userId);
                    return profile.isPresent() && l.getProfileId().equals(profile.get().getId());
                })
                .map(l -> {
                    l.setVisible(req.visible());
                    var saved = linkRepo.save(l);
                    log.info("[PROFILE_CTRL] PUT /me/links/{}/visibility — visible={}", id, req.visible());
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Request records
    // ─────────────────────────────────────────────────────────────────────────

    public record ProfileUpdate(String username, String displayName, String title, String bio, String location,
            String templateId, String mode) {
    }

    public record LinkRequest(@NotBlank String type, String title, @NotBlank String url) {
    }

    public record VisibilityRequest(boolean visible) {
    }
}
