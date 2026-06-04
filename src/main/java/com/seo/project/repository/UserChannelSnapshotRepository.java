package com.seo.project.repository;

import com.seo.project.model.User;
import com.seo.project.model.UserChannelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserChannelSnapshotRepository extends JpaRepository<UserChannelSnapshot, Long> {
    List<UserChannelSnapshot> findByUserOrderByRecordedAtAsc(User user);
    Optional<UserChannelSnapshot> findFirstByUserOrderByRecordedAtDesc(User user);
    boolean existsByUserAndRecordedAtAfter(User user, LocalDateTime recordedAt);
}
