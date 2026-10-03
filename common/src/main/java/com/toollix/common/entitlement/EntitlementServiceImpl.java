package com.toollix.common.entitlement;

import com.toollix.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * DB-backed EntitlementService.
 *
 * A subject (USER or ORG) has a feature if they have at least one subscription
 * that is ACTIVE and not yet expired (expires_at > now OR null = lifetime
 * grant).
 *
 * Feature keys are coarse-grained for now. Later a plan_features join table can
 * gate individual features per plan tier.
 */
@Service
public class EntitlementServiceImpl implements EntitlementService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementServiceImpl.class);

    private final JdbcTemplate jdbc;

    public EntitlementServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Profile entitlements ────────────────────────────────────────────────

    @Override
    public boolean hasFeatureForProfile(Long profileId, String feature) {
        if (profileId == null)
            return false;
        try {
            Long userId = jdbc.queryForObject(
                    "SELECT user_id FROM profiles WHERE id = ?", Long.class, profileId);
            if (userId == null)
                return false;
            Integer count = jdbc.queryForObject(
                    """
                            SELECT COUNT(*) FROM subscriptions
                            WHERE subject_type = 'USER'
                              AND subject_id   = ?
                              AND status       = 'ACTIVE'
                              AND (expires_at IS NULL OR expires_at > ?)
                            """,
                    Integer.class, userId, Instant.now());
            boolean entitled = count != null && count > 0;
            log.debug("[ENTITLEMENT] profileId={} userId={} feature={} entitled={}", profileId, userId, feature,
                    entitled);
            return entitled;
        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (Exception e) {
            log.warn("[ENTITLEMENT] hasFeatureForProfile error profileId={}: {}", profileId, e.getMessage());
            return false;
        }
    }

    @Override
    public void requireProfileFeature(Long profileId, String feature) {
        if (!hasFeatureForProfile(profileId, feature)) {
            log.info("[ENTITLEMENT] access DENIED profileId={} feature={}", profileId, feature);
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED.value(),
                    "upgrade_required: feature '" + feature + "' requires an active subscription");
        }
    }

    // ── Org entitlements ────────────────────────────────────────────────────

    @Override
    public boolean hasFeatureForOrganization(Long organizationId, String feature) {
        if (organizationId == null)
            return false;
        try {
            Integer count = jdbc.queryForObject(
                    """
                            SELECT COUNT(*) FROM subscriptions
                            WHERE subject_type = 'ORG'
                              AND subject_id   = ?
                              AND status       = 'ACTIVE'
                              AND (expires_at IS NULL OR expires_at > ?)
                            """,
                    Integer.class, organizationId, Instant.now());
            boolean entitled = count != null && count > 0;
            log.debug("[ENTITLEMENT] orgId={} feature={} entitled={}", organizationId, feature, entitled);
            return entitled;
        } catch (Exception e) {
            log.warn("[ENTITLEMENT] hasFeatureForOrganization error orgId={}: {}", organizationId, e.getMessage());
            return false;
        }
    }

    @Override
    public void requireOrganizationFeature(Long organizationId, String feature) {
        if (!hasFeatureForOrganization(organizationId, feature)) {
            log.info("[ENTITLEMENT] org access DENIED orgId={} feature={}", organizationId, feature);
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED.value(),
                    "upgrade_required: organization feature '" + feature + "' requires an active subscription");
        }
    }
}
