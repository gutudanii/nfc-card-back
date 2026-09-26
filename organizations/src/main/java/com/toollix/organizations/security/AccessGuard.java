package com.toollix.organizations.security;

import com.toollix.organizations.repo.OrganizationMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Central authorization guard — the ONLY place in the codebase where
 * "can this user perform this action on this resource?" is decided.
 *
 * Used via Spring Method Security:
 * @PreAuthorize("@accessGuard.canEditProfile(#profileId, authentication)")
 * @PreAuthorize("@accessGuard.isOrgRole(#orgId, authentication, 'ADMIN',
 * 'OWNER')")
 * @PreAuthorize("@accessGuard.isPlatformAdmin(authentication)")
 *
 * DESIGN RULE — roles are NEVER baked into the JWT token.
 * The JWT carries only { user_id, token_version }.
 * Roles are resolved at request time from the organization_members table,
 * so membership changes take effect immediately without re-issuing tokens.
 */
@Component("accessGuard")
public class AccessGuard {

    private static final Logger log = LoggerFactory.getLogger(AccessGuard.class);

    private final OrganizationMemberRepository memberRepo;

    public AccessGuard(OrganizationMemberRepository memberRepo) {
        this.memberRepo = memberRepo;
    }

    public boolean canEditProfile(Long profileId, Authentication auth) {
        Long callerId = extractUserId(auth);
        if (callerId == null) {
            log.warn("[ACCESS_GUARD] canEditProfile — unauthenticated caller, profileId={}", profileId);
            return false;
        }
        boolean allowed = callerId.equals(profileId);
        log.debug("[ACCESS_GUARD] canEditProfile — callerId={} profileId={} allowed={}", callerId, profileId, allowed);
        return allowed;
    }

    public boolean isOrgRole(Long orgId, Authentication auth, String... requiredRoles) {
        Long callerId = extractUserId(auth);
        if (callerId == null || orgId == null) {
            log.warn("[ACCESS_GUARD] isOrgRole — null caller or orgId, orgId={}", orgId);
            return false;
        }
        var membership = memberRepo.findByOrgIdAndUserIdAndStatus(orgId, callerId, "ACTIVE");
        if (membership.isEmpty()) {
            log.debug("[ACCESS_GUARD] isOrgRole — callerId={} not a member of orgId={}", callerId, orgId);
            return false;
        }
        String actualRole = membership.get().getRole();
        for (String required : requiredRoles) {
            if (required.equalsIgnoreCase(actualRole)) {
                log.debug("[ACCESS_GUARD] isOrgRole — callerId={} orgId={} role={} ALLOWED", callerId, orgId,
                        actualRole);
                return true;
            }
        }
        log.warn("[ACCESS_GUARD] isOrgRole — callerId={} orgId={} actualRole={} does not satisfy required={} DENIED",
                callerId, orgId, actualRole, String.join(",", requiredRoles));
        return false;
    }

    public boolean isOrgOwner(Long orgId, Authentication auth) {
        return isOrgRole(orgId, auth, "OWNER");
    }

    public boolean isOrgAdmin(Long orgId, Authentication auth) {
        return isOrgRole(orgId, auth, "ADMIN", "OWNER");
    }

    public boolean isPlatformAdmin(Authentication auth) {
        Long callerId = extractUserId(auth);
        if (callerId == null)
            return false;
        log.debug("[ACCESS_GUARD] isPlatformAdmin called for callerId={} — requires UserRepository impl", callerId);
        return false;
    }

    public boolean isOrgMember(Long orgId, Authentication auth) {
        Long callerId = extractUserId(auth);
        if (callerId == null || orgId == null)
            return false;
        boolean member = memberRepo.findByOrgIdAndUserIdAndStatus(orgId, callerId, "ACTIVE").isPresent();
        log.debug("[ACCESS_GUARD] isOrgMember — callerId={} orgId={} result={}", callerId, orgId, member);
        return member;
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getName() == null)
            return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            log.error("[ACCESS_GUARD] Cannot parse userId from principal '{}'", auth.getName());
            return null;
        }
    }
}
