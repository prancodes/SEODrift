package com.seo.project.service;

import com.seo.project.dto.YouTubeVideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChannelHealthEvaluator {

    /**
     * Calculates the overall health score (0-100) based on recent uploads.
     */
    public int calculateHealthScore(List<YouTubeVideoDto> recentUploads, long totalSubscribers) {
        if (recentUploads == null || recentUploads.isEmpty()) {
            return 0; // Needs work, no recent data
        }

        int consistencyScore = calculateConsistency(recentUploads);
        int optimizationScore = calculateOptimization(recentUploads);
        int engagementScore = calculateEngagement(recentUploads, totalSubscribers);

        // Weighted Average
        // Consistency: 30%, Optimization: 30%, Engagement: 40%
        double total = (consistencyScore * 0.3) + (optimizationScore * 0.3) + (engagementScore * 0.4);
        
        log.debug("Health Evaluation: Consistency={}, Optimization={}, Engagement={} -> Total={}", 
                  consistencyScore, optimizationScore, engagementScore, (int) total);
        
        return Math.min(100, (int) total);
    }

    /**
     * Maps the numeric score to a human-readable health status.
     */
    public String getHealthStatus(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 50) return "Needs Work";
        return "Critical";
    }

    private int calculateConsistency(List<YouTubeVideoDto> uploads) {
        // Ideal: 4+ videos in the recent uploads window (usually last 30 days, but here we just look at the list size)
        // If they have 10 recent videos (the max we fetch), they are highly consistent.
        int videoCount = Math.min(uploads.size(), 10);
        return (videoCount * 10); // 10 videos = 100 score
    }

    private int calculateOptimization(List<YouTubeVideoDto> uploads) {
        if (uploads.isEmpty()) return 0;
        
        double avgSeoScore = uploads.stream()
                .mapToInt(YouTubeVideoDto::seoScore)
                .average()
                .orElse(0.0);
                
        return (int) avgSeoScore;
    }

    private int calculateEngagement(List<YouTubeVideoDto> uploads, long totalSubscribers) {
        if (uploads.isEmpty() || totalSubscribers == 0) return 0;
        
        // Calculate average views vs subscribers (Expected 10-15% for a healthy channel)
        double avgViews = uploads.stream()
                .mapToDouble(YouTubeVideoDto::views)
                .average()
                .orElse(0.0);
                
        double viewToSubRatio = (avgViews / (double) totalSubscribers) * 100.0;
        
        // Cap the score: if ratio is >= 15%, perfect score of 100.
        int score = (int) ((viewToSubRatio / 15.0) * 100.0);
        
        return Math.min(100, score);
    }
}
