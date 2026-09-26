package com.toollix.organizations.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organization_members")
public class OrganizationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String role = "MEMBER";

    @Column
    private String department;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "joined_at")
    private Instant joinedAt = Instant.now();

    public OrganizationMember() {}

    public Long getId() { return id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getJoinedAt() { return joinedAt; }
}
