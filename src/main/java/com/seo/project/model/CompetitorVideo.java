package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competitor_videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_channel_id", nullable = false)
    private CompetitorChannel competitorChannel;

    @Column(unique = true, nullable = false)
    private String videoId;

    @Column(nullable = false)
    private String title;

    private LocalDateTime publishedAt;

    private Long viewCount;

    private Long likeCount;
}
