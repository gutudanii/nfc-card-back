package com.toollix.profiles.repo;

import com.toollix.profiles.model.ContactLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactLinkRepository extends JpaRepository<ContactLink, Long> {
    List<ContactLink> findByProfileIdOrderByDisplayOrderAsc(Long profileId);
}
