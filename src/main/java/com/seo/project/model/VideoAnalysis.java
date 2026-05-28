package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoId;
    private String title;
    private String channelTitle;
    private String thumbnailUrl;
    
    @Column(columnDefinition = "TEXT")
    private String videoUrl;

    private Integer seoScore;
    private Double engagementRate;
    private Double sentimentScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onAnalyze() {
        analyzedAt = LocalDateTime.now();
    }
}
