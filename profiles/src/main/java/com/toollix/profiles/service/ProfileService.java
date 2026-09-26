package com.toollix.profiles.service;

import com.toollix.profiles.model.Profile;
import com.toollix.profiles.repo.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository repo;

    public ProfileService(ProfileRepository repo) {
        this.repo = repo;
    }

    public Profile createForUser(Long userId, String username, String displayName) {
        Profile p = new Profile();
        p.setUserId(userId);
        p.setUsername(username);
        p.setDisplayName(displayName);
        return repo.save(p);
    }

    public Optional<Profile> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    public Optional<Profile> findByUserId(Long userId) { return repo.findByUserId(userId); }

    public Profile update(Profile p) { return repo.save(p); }
}
