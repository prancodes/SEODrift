package com.seo.project.repository;

import com.seo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"competitorChannels"})
    Optional<User> findWithCompetitorsByEmail(String email);
}
