package com.seo.project.repository;

import com.seo.project.model.VideoAnalysis;
import com.seo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

import java.util.Optional;
import org.springframework.context.annotation.Lazy;

@Lazy
public interface VideoAnalysisRepository extends JpaRepository<VideoAnalysis, Long> {
    List<VideoAnalysis> findByUserAndIsDeletedFalseOrderByAnalyzedAtDesc(User user);
    long countByUser(User user);
    Optional<VideoAnalysis> findByUserAndVideoIdAndIsDeletedFalse(User user, String videoId);
    Optional<VideoAnalysis> findByIdAndUserAndIsDeletedFalse(Long id, User user);
    
    long countByUserAndAnalyzedAtAfterAndVideoUrlNotLike(User user, LocalDateTime date, String notLikeUrl);
    long countByUserAndAnalyzedAtAfterAndVideoUrlLike(User user, LocalDateTime date, String likeUrl);
}
