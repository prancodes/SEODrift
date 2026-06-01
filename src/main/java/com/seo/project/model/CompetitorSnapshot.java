package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competitor_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_channel_id", nullable = false)
    private CompetitorChannel competitorChannel;

    private Long subscriberCount;

    private Long viewCount;

    private Long videoCount;

    private LocalDateTime recordedAt;

    @PrePersist
    protected void onRecord() {
        recordedAt = LocalDateTime.now();
    }
}
