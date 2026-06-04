package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String googleId;
    private String pictureUrl;

    // YouTube specific cache
    private String youtubeChannelId;
    private String youtubeChannelTitle;
    private String youtubeCustomUrl;
    private String youtubeAvatarUrl;
    private String youtubeUploadsPlaylistId;
    private Long youtubeSubscriberCount;
    private Long youtubeViewCount;
    private Long youtubeVideoCount;
    private LocalDateTime youtubeLastUpdatedAt;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<VideoAnalysis> analyses;

    @ManyToMany
    @JoinTable(
        name = "user_competitors",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "competitor_channel_id")
    )
    private List<CompetitorChannel> competitorChannels;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
