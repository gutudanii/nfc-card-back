package com.toollix.common.entitlement;

import com.toollix.common.web.ApiException;
import org.springframework.http.HttpStatus;

public class EntitlementServiceImpl implements EntitlementService {

    @Override
    public boolean hasFeatureForProfile(Long profileId, String feature) {
        return false;
    }

    @Override
    public boolean hasFeatureForOrganization(Long organizationId, String feature) {
        return false;
    }

    @Override
    public void requireProfileFeature(Long profileId, String feature) {
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "feature_not_allowed");
    }

    @Override
    public void requireOrganizationFeature(Long organizationId, String feature) {
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "feature_not_allowed");
    }
}
