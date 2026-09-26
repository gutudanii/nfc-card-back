package com.toollix.organizations.service;

import com.toollix.common.audit.AuditService;
import com.toollix.organizations.model.Organization;
import com.toollix.organizations.model.OrganizationMember;
import com.toollix.organizations.model.Team;
import com.toollix.organizations.repo.OrganizationMemberRepository;
import com.toollix.organizations.repo.OrganizationRepository;
import com.toollix.organizations.repo.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core organization business logic.
 *
 * Role assignment rules:
 * - The user who creates an org is automatically assigned role=OWNER.
 * - Subsequent members default to role=MEMBER unless overridden by an admin.
 * - Valid roles: MEMBER, TEAM_MANAGER, ADMIN, OWNER.
 */
@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository repo;
    private final OrganizationMemberRepository memberRepo;
    private final TeamRepository teamRepo;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository repo, OrganizationMemberRepository memberRepo,
            TeamRepository teamRepo, AuditService auditService) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.teamRepo = teamRepo;
        this.auditService = auditService;
    }

    @Transactional
    public Organization create(Organization o, Long ownerUserId) {
        log.info("[ORG_SVC] create — name='{}' slug='{}' ownerId={}", o.getName(), o.getSlug(), ownerUserId);
        Organization saved = repo.save(o);
        log.info("[ORG_SVC] create — org persisted id={}", saved.getId());

        // Auto-assign creator as OWNER
        OrganizationMember owner = new OrganizationMember();
        owner.setOrgId(saved.getId());
        owner.setUserId(ownerUserId);
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        memberRepo.save(owner);
        log.info("[ORG_SVC] create — owner membership created userId={} orgId={}", ownerUserId, saved.getId());

        try {
            auditService.record("org.create", String.valueOf(ownerUserId), "id=" + saved.getId());
        } catch (Exception e) {
            log.warn("[AUDIT] org.create failed for id={}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Transactional
    public OrganizationMember addMember(Long orgId, Long userId, String role, String department) {
        log.info("[ORG_SVC] addMember — orgId={} userId={} role={}", orgId, userId, role);

        var existing = memberRepo.findByOrgIdAndUserIdAndStatus(orgId, userId, "ACTIVE");
        if (existing.isPresent()) {
            log.warn("[ORG_SVC] addMember — userId={} already active in orgId={}", userId, orgId);
            throw new IllegalArgumentException("already_a_member");
        }

        String resolvedRole = resolveRole(role);
        var member = new OrganizationMember();
        member.setOrgId(orgId);
        member.setUserId(userId);
        member.setRole(resolvedRole);
        member.setDepartment(department);
        member.setStatus("ACTIVE");
        var saved = memberRepo.save(member);
        log.info("[ORG_SVC] addMember — id={} role={} saved", saved.getId(), resolvedRole);

        try {
            auditService.record("org.addMember", String.valueOf(userId), "orgId=" + orgId + " role=" + resolvedRole);
        } catch (Exception e) {
            log.warn("[AUDIT] org.addMember failed orgId={} userId={}: {}", orgId, userId, e.getMessage());
        }
        return saved;
    }

    public List<OrganizationMember> listMembers(Long orgId) {
        log.debug("[ORG_SVC] listMembers — orgId={}", orgId);
        var list = memberRepo.findByOrgIdAndStatus(orgId, "ACTIVE");
        log.debug("[ORG_SVC] listMembers — orgId={} count={}", orgId, list.size());
        return list;
    }

    @Transactional
    public Team createTeam(Long orgId, String name) {
        log.info("[ORG_SVC] createTeam — orgId={} name='{}'", orgId, name);
        var team = new Team();
        team.setOrgId(orgId);
        team.setName(name);
        var saved = teamRepo.save(team);
        log.info("[ORG_SVC] createTeam — id={}", saved.getId());
        return saved;
    }

    private String resolveRole(String role) {
        if (role == null || role.isBlank())
            return "MEMBER";
        return switch (role.toUpperCase()) {
            case "OWNER", "ADMIN", "TEAM_MANAGER", "MEMBER" -> role.toUpperCase();
            default -> {
                log.warn("[ORG_SVC] Unknown role '{}', defaulting to MEMBER", role);
                yield "MEMBER";
            }
        };
    }

    /** Returns true if caller is an ADMIN or OWNER of the org. */
    public boolean isAdminOrOwner(Long orgId, Long userId) {
        return memberRepo.findByOrgIdAndUserIdAndStatus(orgId, userId, "ACTIVE")
                .map(m -> "OWNER".equals(m.getRole()) || "ADMIN".equals(m.getRole()))
                .orElse(false);
    }

    /** Returns true if caller is the OWNER of the org. */
    public boolean isOwner(Long orgId, Long userId) {
        return memberRepo.findByOrgIdAndUserIdAndStatus(orgId, userId, "ACTIVE")
                .map(m -> "OWNER".equals(m.getRole()))
                .orElse(false);
    }
}
