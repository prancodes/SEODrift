package com.seo.project.repository;

import com.seo.project.model.CompetitorVideo;
import com.seo.project.model.CompetitorChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompetitorVideoRepository extends JpaRepository<CompetitorVideo, Long> {
    Optional<CompetitorVideo> findByVideoId(String videoId);
    List<CompetitorVideo> findByCompetitorChannelOrderByPublishedAtDesc(CompetitorChannel competitorChannel);
}
