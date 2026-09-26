package com.toollix.common.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Component
public class AccessGuard {

    /**
     * Example method used by @PreAuthorize to check edit permission.
     * Real implementation should resolve profile -> owner/org -> membership + role.
     */
    public boolean canEdit(Long profileId, Authentication principal) {
        // Stubbed: allow if principal name equals profileId as string (placeholder)
        if (principal == null || profileId == null) return false;
        try {
            Long userId = Long.parseLong(principal.getName());
            return userId.equals(profileId);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
