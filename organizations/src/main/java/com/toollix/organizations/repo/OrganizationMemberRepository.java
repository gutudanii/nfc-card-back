package com.toollix.organizations.repo;

import com.toollix.organizations.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrgIdAndStatus(Long orgId, String status);
    List<OrganizationMember> findByUserIdAndStatus(Long userId, String status);
    Optional<OrganizationMember> findByOrgIdAndUserId(Long orgId, Long userId);
}
