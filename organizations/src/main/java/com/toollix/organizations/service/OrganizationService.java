package com.toollix.organizations.service;

import com.toollix.organizations.model.Organization;
import com.toollix.organizations.model.OrganizationMember;
import com.toollix.organizations.model.Team;
import com.toollix.organizations.repo.OrganizationMemberRepository;
import com.toollix.organizations.repo.OrganizationRepository;
import com.toollix.organizations.repo.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {
    private final OrganizationRepository repo;
    private final OrganizationMemberRepository memberRepo;
    private final TeamRepository teamRepo;
    private final com.toollix.common.audit.AuditService auditService;

    public OrganizationService(OrganizationRepository repo, OrganizationMemberRepository memberRepo, TeamRepository teamRepo, com.toollix.common.audit.AuditService auditService) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.teamRepo = teamRepo;
        this.auditService = auditService;
    }

    public Organization create(Organization o) {
        var saved = repo.save(o);
        try { auditService.record("org.create", "system", "id="+saved.getId()); } catch (Exception ignored) {}
        return saved;
    }

    public OrganizationMember addMember(Long orgId, Long userId, String role, String department) {
        var member = new OrganizationMember();
        member.setOrgId(orgId);
        member.setUserId(userId);
        member.setRole(role == null || role.isBlank() ? "MEMBER" : role);
        member.setDepartment(department);
        member.setStatus("ACTIVE");
        return memberRepo.save(member);
    }

    public List<OrganizationMember> listMembers(Long orgId) {
        return memberRepo.findByOrgIdAndStatus(orgId, "ACTIVE");
    }

    public Team createTeam(Long orgId, String name) {
        var team = new Team();
        team.setOrgId(orgId);
        team.setName(name);
        return teamRepo.save(team);
    }
}
