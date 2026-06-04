package com.seo.project.dto;

import java.util.List;
import java.util.Map;

/**
 * YouTubeChannelDto represents the authenticated user's YouTube channel metrics
 * and their geographic viewer distribution (if analytics are enabled).
 */
public record YouTubeChannelDto(
    String channelId,
    String title,
    String customUrl,
    String avatarUrl,
    String uploadsPlaylistId,
    long subscriberCount,
    long viewCount,
    long videoCount,
    String country,
    Map<String, Double> geoDistribution,
    List<YouTubeVideoDto> recentUploads
) {}
