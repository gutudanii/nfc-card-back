package com.toollix.profiles;

import com.toollix.profiles.model.Profile;
import com.toollix.profiles.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/p/{username}")
    public ResponseEntity<?> publicProfile(@PathVariable String username) {
        return profileService.findByUsername(username)
                .map(p -> ResponseEntity.ok(p))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String principal, @RequestBody ProfileUpdate req) {
        Long userId = Long.parseLong(principal);
        var maybe = profileService.findByUserId(userId);
        Profile p;
        if (maybe.isEmpty()) {
            p = profileService.createForUser(userId, req.username, req.displayName);
        } else {
            p = maybe.get();
            p.setDisplayName(req.displayName);
            p.setUsername(req.username);
            p = profileService.update(p);
        }
        return ResponseEntity.ok(p);
    }

    public static record ProfileUpdate(String username, String displayName) {}
}
