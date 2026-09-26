package com.toollix.common.entitlement;

public interface EntitlementService {
    boolean hasFeatureForProfile(Long profileId, String feature);
    boolean hasFeatureForOrganization(Long organizationId, String feature);
    void requireProfileFeature(Long profileId, String feature);
    void requireOrganizationFeature(Long organizationId, String feature);
}
