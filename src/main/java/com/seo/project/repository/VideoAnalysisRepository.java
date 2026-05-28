package com.seo.project.repository;

import com.seo.project.model.VideoAnalysis;
import com.seo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

public interface VideoAnalysisRepository extends JpaRepository<VideoAnalysis, Long> {
    List<VideoAnalysis> findByUserOrderByAnalyzedAtDesc(User user);
    long countByUser(User user);
    Optional<VideoAnalysis> findByUserAndVideoId(User user, String videoId);
}
