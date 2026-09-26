package com.toollix.organizations.repo;

import com.toollix.organizations.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOrgId(Long orgId);
}
