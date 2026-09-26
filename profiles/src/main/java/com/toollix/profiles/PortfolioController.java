package com.toollix.profiles;

import com.toollix.common.storage.FileStorageService;
import com.toollix.profiles.model.PortfolioItem;
import com.toollix.profiles.repo.PortfolioItemRepository;
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

@RestController
@RequestMapping("/me/portfolio")
public class PortfolioController {
    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final ProfileService profileService;
    private final PortfolioItemRepository repo;
    private final FileStorageService storageService;

    public PortfolioController(ProfileService profileService, PortfolioItemRepository repo,
            FileStorageService storageService) {
        this.profileService = profileService;
        this.repo = repo;
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<?> getMyPortfolio(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        var profile = profileService.findByUserId(userId);
        if (profile.isEmpty())
            return ResponseEntity.ok(List.of());

        var items = repo.findByProfileIdOrderByDisplayOrderAsc(profile.get().getId());
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> addPortfolioItem(@AuthenticationPrincipal String principal,
            @Valid @RequestBody PortfolioItemRequest req) {
        Long userId = Long.parseLong(principal);
        var p = profileService.findByUserId(userId)
                .orElseGet(() -> profileService.createForUser(userId, "user" + userId, "New User"));

        int order = repo.findByProfileIdOrderByDisplayOrderAsc(p.getId()).size();

        var item = new PortfolioItem();
        item.setProfileId(p.getId());
        item.setItemType(req.itemType());
        item.setTitle(req.title());
        item.setCategory(req.category());
        item.setPriceText(req.priceText());
        item.setExternalUrl(req.externalUrl());
        item.setDisplayOrder(order);
        var saved = repo.save(item);
        log.info("[PORTFOLIO_CTRL] POST /me/portfolio — userId={} type={}", userId, req.itemType());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortfolioItem(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        return repo.findById(id)
                .filter(i -> {
                    var profile = profileService.findByUserId(userId);
                    return profile.isPresent() && i.getProfileId().equals(profile.get().getId());
                })
                .map(i -> {
                    repo.delete(i);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<?> uploadCover(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal,
            @RequestParam("file") MultipartFile file) {
        Long userId = Long.parseLong(principal);
        return repo.findById(id)
                .filter(i -> {
                    var profile = profileService.findByUserId(userId);
                    return profile.isPresent() && i.getProfileId().equals(profile.get().getId());
                })
                .map(i -> {
                    String url = storageService.storeFile(file);
                    i.setImageUrl(url);
                    repo.save(i);
                    return ResponseEntity.ok(Map.of("imageUrl", url));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PortfolioItemRequest(
            @NotBlank String itemType,
            @NotBlank String title,
            String category,
            String priceText,
            String externalUrl) {
    }
}
