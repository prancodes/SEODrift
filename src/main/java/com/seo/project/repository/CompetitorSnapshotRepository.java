package com.seo.project.repository;

import com.seo.project.model.CompetitorSnapshot;
import com.seo.project.model.CompetitorChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompetitorSnapshotRepository extends JpaRepository<CompetitorSnapshot, Long> {
    List<CompetitorSnapshot> findByCompetitorChannelOrderByRecordedAtDesc(CompetitorChannel competitorChannel);
}
