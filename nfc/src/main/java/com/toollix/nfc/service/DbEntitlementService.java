package com.toollix.nfc.service;

import com.toollix.common.entitlement.EntitlementService;
import com.toollix.common.web.ApiException;
import com.toollix.profiles.model.Profile;
import com.toollix.profiles.repo.ProfileRepository;
import com.toollix.subscriptions.model.Plan;
import com.toollix.subscriptions.model.Subscription;
import com.toollix.subscriptions.repo.PlanRepository;
import com.toollix.subscriptions.repo.SubscriptionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Primary
@Service
public class DbEntitlementService implements EntitlementService {

    private final ProfileRepository profileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public DbEntitlementService(ProfileRepository profileRepository,
                               SubscriptionRepository subscriptionRepository,
                               PlanRepository planRepository) {
        this.profileRepository = profileRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Override
    public boolean hasFeatureForProfile(Long profileId, String feature) {
        if (profileId == null || feature == null || feature.isBlank()) return false;
        Optional<Profile> profile = profileRepository.findById(profileId);
        if (profile.isEmpty()) return false;
        return hasFeatureForUser(profile.get().getUserId(), feature);
    }

    @Override
    public boolean hasFeatureForOrganization(Long organizationId, String feature) {
        if (organizationId == null || feature == null || feature.isBlank()) return false;
        List<Subscription> subs = subscriptionRepository.findAll().stream()
                .filter(it -> "ORGANIZATION".equalsIgnoreCase(it.getSubjectType()))
                .filter(it -> it.getSubjectId() != null && it.getSubjectId().equals(organizationId))
                .filter(it -> it.getStatus() != null && "ACTIVE".equalsIgnoreCase(it.getStatus()))
                .filter(it -> it.getExpiresAt() == null || it.getExpiresAt().isAfter(Instant.now()))
                .toList();
        return subs.stream().anyMatch(sub -> planAllowsFeature(sub.getPlanId(), feature));
    }

    @Override
    public void requireProfileFeature(Long profileId, String feature) {
        if (!hasFeatureForProfile(profileId, feature)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "feature_not_allowed");
        }
    }

    @Override
    public void requireOrganizationFeature(Long organizationId, String feature) {
        if (!hasFeatureForOrganization(organizationId, feature)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "feature_not_allowed");
        }
    }

    private boolean hasFeatureForUser(Long userId, String feature) {
        if (userId == null || feature == null || feature.isBlank()) return false;
        List<Subscription> subs = subscriptionRepository.findAll().stream()
                .filter(it -> "USER".equalsIgnoreCase(it.getSubjectType()))
                .filter(it -> it.getSubjectId() != null && it.getSubjectId().equals(userId))
                .filter(it -> it.getStatus() != null && "ACTIVE".equalsIgnoreCase(it.getStatus()))
                .filter(it -> it.getExpiresAt() == null || it.getExpiresAt().isAfter(Instant.now()))
                .toList();

        return subs.stream().anyMatch(sub -> planAllowsFeature(sub.getPlanId(), feature));
    }

    private boolean planAllowsFeature(Long planId, String feature) {
        if (planId == null || feature == null || feature.isBlank()) return false;
        Optional<Plan> plan = planRepository.findById(planId);
        if (plan.isEmpty()) return false;
        String features = plan.get().getFeatures();
        if (features == null || features.isBlank()) return false;
        return features.contains(feature);
    }
}
