package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "picture_url")
    private String pictureUrl;

    // YouTube specific cache
    @Column(name = "youtube_channel_id")
    private String youtubeChannelId;

    @Column(name = "youtube_channel_title")
    private String youtubeChannelTitle;

    @Column(name = "youtube_custom_url")
    private String youtubeCustomUrl;

    @Column(name = "youtube_avatar_url")
    private String youtubeAvatarUrl;

    @Column(name = "youtube_uploads_playlist_id")
    private String youtubeUploadsPlaylistId;

    @Column(name = "youtube_subscriber_count")
    private Long youtubeSubscriberCount;

    @Column(name = "youtube_view_count")
    private Long youtubeViewCount;

    @Column(name = "youtube_video_count")
    private Long youtubeVideoCount;

    @Column(name = "youtube_watch_time")
    private Long youtubeWatchTime;

    @Column(name = "youtube_impressions")
    private Long youtubeImpressions;

    @Column(name = "youtube_ctr")
    private Double youtubeCtr;

    @Column(name = "youtube_last_updated_at")
    private LocalDateTime youtubeLastUpdatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "dodo_customer_id")
    private String dodoCustomerId;

    @Column(name = "dodo_subscription_id")
    private String dodoSubscriptionId;

    @Builder.Default
    @Column(name = "role")
    private String role = "ROLE_USER";

    @Builder.Default
    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Builder.Default
    @Column(name = "ai_generations_used")
    private Integer aiGenerationsUsed = 0;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<VideoAnalysis> analyses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedKeyword> savedKeywords = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserChannelSnapshot> channelSnapshots = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "user_competitors",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "competitor_channel_id")
    )
    private List<CompetitorChannel> competitorChannels = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
