package com.toollix.profiles.repo;

import com.toollix.profiles.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByProfileIdOrderByDisplayOrderAsc(Long profileId);
}
