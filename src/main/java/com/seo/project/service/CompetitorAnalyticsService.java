package com.seo.project.service;

import com.seo.project.model.*;
import com.seo.project.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompetitorAnalyticsService {

    private final CompetitorVideoRepository competitorVideoRepository;
    private final CompetitorSnapshotRepository competitorSnapshotRepository;
    private final UserChannelSnapshotRepository userChannelSnapshotRepository;
    private final KeywordTrendRepository keywordTrendRepository;
    private final WebClient webClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${base.url}")
    private String youtubeApiBaseUrl;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "and", "in", "to", "of", "for", "with", "how", "you", "your", "on", "is", "this", "that", 
            "it", "at", "by", "from", "an", "as", "are", "be", "or", "what", "why", "who", "which", "where", 
            "when", "my", "me", "we", "us", "they", "them", "he", "she", "him", "her", "i", "can", "will", 
            "do", "does", "did", "have", "has", "had", "but", "so", "up", "out", "about", "into", "no", "yes",
            "not", "new", "all", "get", "make", "video", "youtube", "tutorial", "best", "top", "free", "easy"
    ));

    public CompetitorAnalyticsService(CompetitorVideoRepository competitorVideoRepository,
                                      CompetitorSnapshotRepository competitorSnapshotRepository,
                                      UserChannelSnapshotRepository userChannelSnapshotRepository,
                                      KeywordTrendRepository keywordTrendRepository,
                                      WebClient.Builder webClientBuilder) {
        this.competitorVideoRepository = competitorVideoRepository;
        this.competitorSnapshotRepository = competitorSnapshotRepository;
        this.userChannelSnapshotRepository = userChannelSnapshotRepository;
        this.keywordTrendRepository = keywordTrendRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Calculates the posting rhythm (day of the week and hour of the day distribution) 
     * of competitor videos.
     */
    public Map<String, Object> calculatePostingRhythm(List<CompetitorChannel> competitors) {
        Map<String, Integer> dayCounts = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayCounts.put(day.name(), 0);
        }

        Map<Integer, Integer> hourCounts = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            hourCounts.put(h, 0);
        }

        for (CompetitorChannel competitor : competitors) {
            List<CompetitorVideo> videos = competitorVideoRepository.findByCompetitorChannelOrderByPublishedAtDesc(competitor);
            for (CompetitorVideo video : videos) {
                if (video.getPublishedAt() != null) {
                    LocalDateTime publishedAt = video.getPublishedAt();
                    String dayName = publishedAt.getDayOfWeek().name();
                    dayCounts.put(dayName, dayCounts.getOrDefault(dayName, 0) + 1);

                    int hour = publishedAt.getHour();
                    hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
                }
            }
        }

        Map<String, Object> rhythm = new HashMap<>();
        rhythm.put("days", dayCounts);
        rhythm.put("hours", hourCounts);
        return rhythm;
    }

    /**
     * Computes the "Topic Momentum" based on the frequency of keywords in titles of competitor videos.
     */
    public List<Map<String, Object>> calculateTopicMomentum(List<CompetitorChannel> competitors) {
        Map<String, Integer> termFrequencies = new HashMap<>();

        for (CompetitorChannel competitor : competitors) {
            List<CompetitorVideo> videos = competitorVideoRepository.findByCompetitorChannelOrderByPublishedAtDesc(competitor);
            for (CompetitorVideo video : videos) {
                if (video.getTitle() == null) continue;
                String cleanTitle = video.getTitle().toLowerCase()
                        .replaceAll("[^a-zA-Z0-9\\s]", "");
                
                String[] words = cleanTitle.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                        termFrequencies.put(word, termFrequencies.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        return termFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("term", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Formats subscriber growth comparison datasets for Chart.js.
     */
    public Map<String, Object> getSubscriberBenchmarkingData(User user, List<CompetitorChannel> competitors) {
        Map<String, Object> benchmark = new HashMap<>();
        
        List<Map<String, Object>> datasets = new ArrayList<>();

        // 1. Add User's own data
        List<UserChannelSnapshot> userSnapshots = userChannelSnapshotRepository.findByUserOrderByRecordedAtAsc(user);
        Map<String, Object> userDataset = new HashMap<>();
        userDataset.put("label", user.getYoutubeChannelTitle() != null ? user.getYoutubeChannelTitle() : "My Channel");
        userDataset.put("borderColor", "#6366f1"); // Indigo
        userDataset.put("backgroundColor", "rgba(99, 102, 241, 0.1)");
        
        List<Map<String, Object>> userPoints = userSnapshots.stream().map(s -> {
            Map<String, Object> pt = new HashMap<>();
            pt.put("x", s.getRecordedAt().toString().substring(0, 10));
            pt.put("y", s.getSubscriberCount());
            return pt;
        }).collect(Collectors.toList());
        userDataset.put("data", userPoints);
        datasets.add(userDataset);

        // Colors for competitors
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6", "#06b6d4"};
        int colorIdx = 0;

        // 2. Add Competitors
        for (CompetitorChannel competitor : competitors) {
            List<CompetitorSnapshot> snapshots = competitorSnapshotRepository.findByCompetitorChannelOrderByRecordedAtDesc(competitor);
            // Create a temporary list to reverse safely
            List<CompetitorSnapshot> chronologicalSnapshots = new ArrayList<>(snapshots);
            Collections.reverse(chronologicalSnapshots);

            Map<String, Object> compDataset = new HashMap<>();
            compDataset.put("label", competitor.getTitle());
            compDataset.put("borderColor", colors[colorIdx % colors.length]);
            compDataset.put("backgroundColor", "transparent");
            colorIdx++;

            List<Map<String, Object>> compPoints = chronologicalSnapshots.stream().map(s -> {
                Map<String, Object> pt = new HashMap<>();
                pt.put("x", s.getRecordedAt().toString().substring(0, 10));
                pt.put("y", s.getSubscriberCount());
                return pt;
            }).collect(Collectors.toList());
            compDataset.put("data", compPoints);
            datasets.add(compDataset);
        }

        benchmark.put("datasets", datasets);
        return benchmark;
    }

    /**
     * Queries the YouTube Search API to retrieve publication counts for a keyword this month vs last month,
     * calculating its keyword velocity/growth rate and saving a KeywordTrend record.
     */
    public KeywordTrend fetchKeywordTrend(String keyword) {
        try {
            // This month (last 30 days)
            String nowIso = ZonedDateTime.now().toInstant().toString();
            String thirtyDaysAgoIso = ZonedDateTime.now().minusDays(30).toInstant().toString();
            String sixtyDaysAgoIso = ZonedDateTime.now().minusDays(60).toInstant().toString();

            // 1. Search count for this month
            String urlThisMonth = youtubeApiBaseUrl + "/search?q=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8)
                    + "&publishedAfter=" + thirtyDaysAgoIso
                    + "&publishedBefore=" + nowIso
                    + "&type=video&key=" + apiKey;
            
            JsonNode rootThisMonth = webClient.get().uri(urlThisMonth).retrieve().bodyToMono(JsonNode.class).block();
            long countThisMonth = 0;
            if (rootThisMonth != null && rootThisMonth.has("pageInfo")) {
                countThisMonth = rootThisMonth.get("pageInfo").get("totalResults").asLong();
            }

            // 2. Search count for last month
            String urlLastMonth = youtubeApiBaseUrl + "/search?q=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8)
                    + "&publishedAfter=" + sixtyDaysAgoIso
                    + "&publishedBefore=" + thirtyDaysAgoIso
                    + "&type=video&key=" + apiKey;
            
            JsonNode rootLastMonth = webClient.get().uri(urlLastMonth).retrieve().bodyToMono(JsonNode.class).block();
            long countLastMonth = 0;
            if (rootLastMonth != null && rootLastMonth.has("pageInfo")) {
                countLastMonth = rootLastMonth.get("pageInfo").get("totalResults").asLong();
            }

            double growthRate = 0.0;
            if (countLastMonth > 0) {
                growthRate = ((double)(countThisMonth - countLastMonth) / countLastMonth) * 100.0;
            } else if (countThisMonth > 0) {
                growthRate = 100.0; // Infinite/100% growth
            }

            // Clean up existing trend records for same day to prevent duplicates
            keywordTrendRepository.findByKeywordAndRecordedDate(keyword, LocalDate.now()).ifPresent(keywordTrendRepository::delete);

            KeywordTrend trend = KeywordTrend.builder()
                    .keyword(keyword)
                    .videoCountLastMonth((int)countLastMonth)
                    .videoCountThisMonth((int)countThisMonth)
                    .growthRate(growthRate)
                    .recordedDate(LocalDate.now())
                    .build();

            return keywordTrendRepository.save(trend);
        } catch (Exception e) {
            log.error("Failed to fetch search trends for keyword {}: {}", keyword, e.getMessage());
            // Persist a zeroed trend so subsequent page loads don't retry the live API
            // (which would burn quota on every request after a transient failure)
            KeywordTrend zeroTrend = KeywordTrend.builder()
                    .keyword(keyword)
                    .videoCountLastMonth(0)
                    .videoCountThisMonth(0)
                    .growthRate(0.0)
                    .recordedDate(LocalDate.now())
                    .build();
            return keywordTrendRepository.save(zeroTrend);
        }
    }

    /**
     * Calculates baseline average views for competitor channels and identifies viral outliers
     * (Performance Multiplier > 3.0x).
     */
    public List<Map<String, Object>> findViralOutliers(List<CompetitorChannel> competitors) {
        List<Map<String, Object>> outliers = new ArrayList<>();
        
        for (CompetitorChannel competitor : competitors) {
            List<CompetitorVideo> videos = competitorVideoRepository.findByCompetitorChannelOrderByPublishedAtDesc(competitor);
            if (videos.isEmpty()) continue;
            
            // Calculate baseline average views
            double totalViews = 0;
            int validVideos = 0;
            for (CompetitorVideo video : videos) {
                if (video.getViewCount() != null) {
                    totalViews += video.getViewCount();
                    validVideos++;
                }
            }
            
            if (validVideos == 0) continue;
            double averageViews = totalViews / validVideos;
            
            // Find outliers
            for (CompetitorVideo video : videos) {
                if (video.getViewCount() != null && averageViews > 0) {
                    double multiplier = video.getViewCount() / averageViews;
                    if (multiplier >= 3.0) {
                        Map<String, Object> outlierMap = new HashMap<>();
                        outlierMap.put("videoId", video.getVideoId());
                        outlierMap.put("title", video.getTitle());
                        outlierMap.put("competitorTitle", competitor.getTitle());
                        outlierMap.put("viewCount", video.getViewCount());
                        outlierMap.put("publishedAt", video.getPublishedAt());
                        outlierMap.put("multiplier", String.format("%.1fx", multiplier));
                        outlierMap.put("rawMultiplier", multiplier);
                        outliers.add(outlierMap);
                    }
                }
            }
        }
        
        // Sort by multiplier descending
        outliers.sort((a, b) -> Double.compare(
                (Double) b.get("rawMultiplier"),
                (Double) a.get("rawMultiplier")
        ));
        
        return outliers.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Cross-references keyword search velocity with competitor video titles.
     * Identifies terms with high demand but low competitor coverage as Content Goldmines.
     */
    public List<Map<String, Object>> findContentGaps(User user, List<String> savedKeywords) {
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<CompetitorChannel> competitors = user.getCompetitorChannels();
        
        if (competitors == null || competitors.isEmpty()) return gaps;

        for (String keyword : savedKeywords) {
            // Get latest trend
            List<KeywordTrend> trends = keywordTrendRepository.findByKeywordOrderByRecordedDateDesc(keyword);
            if (trends.isEmpty()) continue;
            
            KeywordTrend latestTrend = trends.get(0);
            
            // Criteria for "High Demand"
            if (latestTrend.getVideoCountThisMonth() > 500 || latestTrend.getGrowthRate() > 20.0) {
                // Check competitor coverage
                long coverage = competitorVideoRepository.countByCompetitorChannelInAndTitleContainingIgnoreCase(competitors, keyword);
                
                // If coverage is low
                if (coverage <= 1) {
                    Map<String, Object> gap = new HashMap<>();
                    gap.put("keyword", keyword);
                    gap.put("searchVelocity", latestTrend.getVideoCountThisMonth());
                    gap.put("growthRate", String.format("%.1f", latestTrend.getGrowthRate()));
                    gap.put("competitorCoverage", coverage);
                    gaps.add(gap);
                }
            }
        }
        
        // Sort gaps by highest search velocity
        gaps.sort((a, b) -> Integer.compare((Integer) b.get("searchVelocity"), (Integer) a.get("searchVelocity")));
        return gaps;
    }
}
