package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "competitor_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String channelId;

    private String customUrl;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    private Long subscriberCount;

    private Long viewCount;

    private Long videoCount;

    private LocalDateTime lastScrapedAt;

    @ManyToMany(mappedBy = "competitorChannels")
    private List<User> users;

    @OneToMany(mappedBy = "competitorChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitorSnapshot> snapshots;

    @OneToMany(mappedBy = "competitorChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitorVideo> videos;
}
