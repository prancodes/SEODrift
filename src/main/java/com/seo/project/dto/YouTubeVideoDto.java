package com.seo.project.dto;

/**
 * YouTubeVideoDto represents an individual video's metrics and SEO quality points,
 * primarily used for tracking the user's recent uploads performance.
 */
public record YouTubeVideoDto(
    String videoId,
    String title,
    String publishedAt,
    String thumbnailUrl,
    long views,
    long likes,
    long comments,
    double engagementRate,
    int seoScore
) {}
