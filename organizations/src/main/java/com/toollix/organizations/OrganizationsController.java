package com.toollix.organizations;

import com.toollix.organizations.model.Organization;
import com.toollix.organizations.model.OrganizationMember;
import com.toollix.organizations.model.Team;
import com.toollix.organizations.repo.OrganizationRepository;
import com.toollix.organizations.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orgs")
public class OrganizationsController {
    private final OrganizationService service;
    private final OrganizationRepository repo;
    public OrganizationsController(OrganizationService service, OrganizationRepository repo) { this.service = service; this.repo = repo; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Organization o) { return ResponseEntity.ok(service.create(o)); }

    @GetMapping("/{slug}")
    public ResponseEntity<?> get(@PathVariable String slug) { return repo.findBySlug(slug).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long orgId, @RequestBody OrganizationMember member) {
        return ResponseEntity.ok(service.addMember(orgId, member.getUserId(), member.getRole(), member.getDepartment()));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<?> members(@PathVariable Long orgId) { return ResponseEntity.ok(service.listMembers(orgId)); }

    @PostMapping("/{orgId}/teams")
    public ResponseEntity<?> createTeam(@PathVariable Long orgId, @RequestBody Team team) {
        return ResponseEntity.ok(service.createTeam(orgId, team.getName()));
    }
}
