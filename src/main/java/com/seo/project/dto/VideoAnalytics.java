package com.seo.project.dto;

import java.util.List;

public record VideoAnalytics(
    String videoId,
    String title,
    String channelName,
    String thumbnailUrl,
    String publishedAt,
    
    // Raw Stats
    long viewCount,
    Long likeCount,
    long dislikeCount, // Fetched from RYD API
    long commentCount,
    
    // Calculated Metrics
    Double engagementRate, // (Likes + Comments) / Views * 100
    Double sentimentScore, // Likes / (Likes + Dislikes) * 100
    
    // Hidden Flag
    Boolean likesHidden,
    
    // SEO Audit
    int seoScore, // 0 to 100
    List<AuditResult> auditResults
) {
    public record AuditResult(boolean passed, String message) {}
}