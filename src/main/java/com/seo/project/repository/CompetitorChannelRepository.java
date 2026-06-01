package com.seo.project.repository;

import com.seo.project.model.CompetitorChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompetitorChannelRepository extends JpaRepository<CompetitorChannel, Long> {
    Optional<CompetitorChannel> findByChannelId(String channelId);
}
