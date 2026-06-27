package com.seo.project.service;

import com.seo.project.dto.AiGenerationDto;
import com.seo.project.dto.KeywordAiAnalysisDto;
import com.seo.project.dto.VideoAiAuditDto;
import com.seo.project.dto.VideoAiAuditDto.AuditItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * AiWorkspaceService coordinates structured prompts and invokes Google Gemini
 * through Spring AI to generate optimized video metadata.
 */
@Slf4j
@Service
public class AiWorkspaceService {

    private final ChatClient chatClient;

    public AiWorkspaceService(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    /**
     * Calls Gemini via Spring AI to generate optimized YouTube content ideas, titles, descriptions,
     * tags, hooks, and video chapters.
     */
    public AiGenerationDto generateWorkspaceContent(
            String topic, 
            String tone, 
            List<String> competitorTitles, 
            List<String> competitorTags, 
            String creatorChannelName,
            String creatorStyleContext) {
        
        log.info("Generating AI content workspace for topic: '{}', tone: '{}', user channel: '{}'", 
                topic, tone, creatorChannelName);

        String systemPrompt = """
            You are a senior YouTube SEO specialist, script writer, and metadata optimizer.
            Your task is to generate highly optimized, click-worthy, and algorithm-friendly metadata and structure for a video.
            
            Strict requirements:
            Strict requirements:
            1. You MUST generate 3 title suggestions that are optimized for high Click-Through-Rate (CTR) and search indexability.
            2. The description must be highly engaging, start with a hook, include placeholders for links/Call-To-Actions (CTAs) in bracket format like [Insert Link Here], and naturally incorporate keyword variants. IMPORTANT: DO NOT include the timestamps or chapters in the description body. Use proper spacing, paragraph breaks (double newlines), and emojis to ensure the description is highly readable and visually appealing. Do not output a single wall of text.
            3. Provide a hook / intro script outline to grab attention in the first 15 seconds.
            4. Recommend exactly 15 to 20 relevant tags and 3 to 5 trending hashtags.
            5. Create an outline of recommended chapters (timestamp and title) based on standard pacing for the target topic. Provide these ONLY in the chapters JSON array.
            
            Style Context (MATCH THIS CREATOR'S STYLE):
            Below are previous high-performing titles and descriptions from this creator. You must mimic their formatting, phrasing, tone, and pacing in your generated descriptions and outlines.
            {creatorStyleContext}
            
            You must format the response as a strict JSON structure matching the schema instructions.
            """;

        String userPrompt = """
            Generate video ideas and metadata for:
            Topic: {topic}
            Requested Tone: {tone}
            Creator Channel Name: {creatorChannelName}
            
            Competitor Context (Use these for reference to find gaps and style inspiration):
            - Top Ranking Video Titles: {competitorTitles}
            - Common Tags: {competitorTags}
            
            Please create the full optimized video metadata package.
            """;

        try {
            return chatClient.prompt()
                    .system(s -> s.text(systemPrompt)
                            .param("creatorStyleContext", creatorStyleContext != null && !creatorStyleContext.isEmpty() ? creatorStyleContext : "No specific style history available. Use best practices.")
                    )
                    .user(u -> u.text(userPrompt)
                            .param("topic", topic)
                            .param("tone", tone != null ? tone : "engaging")
                            .param("creatorChannelName", creatorChannelName != null ? creatorChannelName : "Creator")
                            .param("competitorTitles", competitorTitles != null && !competitorTitles.isEmpty() ? String.join(", ", competitorTitles) : "None")
                            .param("competitorTags", competitorTags != null && !competitorTags.isEmpty() ? String.join(", ", competitorTags) : "None")
                    )
                    .call()
                    .entity(AiGenerationDto.class);
        } catch (Exception e) {
            log.error("Error invoking Gemini API via Spring AI", e);
            throw new RuntimeException("Failed to generate AI content workspace: " + e.getMessage(), e);
        }
    }

    /**
     * Calls Gemini via Spring AI to perform a dynamic and contextual analysis of the keyword,
     * its monthly volume, and growth rate, returning a structured analysis object.
     */
    public KeywordAiAnalysisDto analyzeKeyword(
            String keyword,
            int videoCountThisMonth,
            double growthRate) {
        
        log.info("Generating dynamic SEO analytics for keyword: '{}', monthly volume: {}, growth: {}%", 
                keyword, videoCountThisMonth, growthRate);

        String systemPrompt = """
            You are an advanced YouTube SEO strategist. Your job is to analyze a search term's competitive landscape
            based on its monthly video publication volume and recent percentage growth.
            
            Based on the context, you must output a structured analysis:
            1. difficulty: Choose one of: "Easy", "Moderate", "Hard", "Ultra High".
            2. competitionAdvice: Give specific, context-aware competitive advice for creators. Evaluate if the query is a long-tail version and how easy it is to rank. 
               Format this as a rich, structured, beautiful Markdown response (like a README). DO NOT return a single plain paragraph. Use:
               - Subheadings (###) to separate sections (e.g. ### Landscape Overview, ### Competitive Pivot)
               - Bold text (**important**) to highlight core concepts
               - Bullet points (- item) to list recommended long-tail queries or target segments
               - DO NOT mention "capped" or API limits.
            3. growthPotential: State the growth trend dynamically, e.g., "Declining (-10.9%)", "Stable (+0.0%)", "Exponential (+45.2%)", or "Sustained High Interest".
            4. seoAdvice: Give actionable, highly practical SEO optimization advice tailored specifically to this topic.
               Format this as a rich, structured, beautiful Markdown response (like a README). DO NOT return a single plain paragraph. Use:
               - Subheadings (###) for category organization (e.g. ### Title Blueprint, ### Description Hook, ### Tag Strategy)
               - Bold text (**important**) to emphasize priority tasks
               - Numbered list (1. item) or bullet points for step-by-step action items
            
            You must format the response as a strict JSON structure matching the schema instructions.
            """;

        String userPrompt = """
            Perform keyword SEO analysis for:
            Keyword: {keyword}
            Monthly Videos Published: {volume}
            Growth Rate: {growth}%
            """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt)
                            .param("keyword", keyword)
                            .param("volume", String.valueOf(videoCountThisMonth))
                            .param("growth", String.format("%.1f", growthRate))
                    )
                    .call()
                    .entity(KeywordAiAnalysisDto.class);
        } catch (Exception e) {
            log.error("Error invoking Gemini for keyword analysis", e);
            // Fallback strategy if API call fails
            String diff = "Easy";
            String advice = """
                ### Landscape Overview
                Low competition opportunity. This tag presents a **high probability** of ranking quickly.
                
                ### Pivot Suggestions
                - Create focused, step-by-step tutorials
                - Optimize for direct user intent rather than broad queries
                """;
            if (videoCountThisMonth > 500000) {
                diff = "Ultra High";
                advice = """
                    ### Landscape Overview
                    Ultra-high volume broad term. The market is **extremely saturated** by massive channels.
                    
                    ### Pivot Suggestions
                    - Target long-tail variations of this keyword
                    - Focus on specific, ultra-niche audience segments
                    """;
            } else if (videoCountThisMonth > 2000) {
                diff = "Hard";
                advice = """
                    ### Landscape Overview
                    Highly competitive search term. Established authority channels currently **dominate search results**.
                    
                    ### Pivot Suggestions
                    - Target specific, multi-word long-tail queries
                    - Use high-CTR, highly distinct title formats
                    """;
            } else if (videoCountThisMonth > 500) {
                diff = "Moderate";
                advice = """
                    ### Landscape Overview
                    Moderate competition landscape. A **well-optimized video** has a very healthy chance to rank.
                    
                    ### Pivot Suggestions
                    - Design custom, high-contrast thumbnails
                    - Optimize description tags within the first 2 sentences
                    """;
            }
            
            String growthText = "Stable (+0.0%)";
            if (growthRate > 15.0) {
                growthText = "Exponential (+" + String.format("%.1f", growthRate) + "%)";
            } else if (growthRate < -5.0) {
                growthText = "Declining (" + String.format("%.1f", growthRate) + "%)";
            } else if (growthRate > 0.0) {
                growthText = "Growing (+" + String.format("%.1f", growthRate) + "%)";
            }
            
            String fallbackSeo = """
                ### Title Blueprint
                Use **outcome-based** video titles that highlight a clear benefit or specific target group.
                
                ### Description Hook
                Include key terms and long-tail variants within the first 150 characters to align with search algorithms.
                
                ### Action Checklist
                1. Research primary tags and search intents
                2. Design high-contrast visual hooks
                3. Place key timestamps in the video timeline
                """;
            
            return new KeywordAiAnalysisDto(
                    diff,
                    advice,
                    growthText,
                    fallbackSeo
            );
        }
    }

    /**
     * Calls Gemini via Spring AI to perform a deep SEO health audit on a video's metadata.
     */
    public VideoAiAuditDto generateVideoAudit(String title, String description, List<String> tags) {
        log.info("Generating dynamic SEO audit for video title: '{}'", title);

        String systemPrompt = """
            You are an elite YouTube SEO Auditor. Your job is to analyze a video's metadata (Title, Description, and Tags)
            and provide a critical, actionable SEO Health Audit.

            You must output a structured JSON response matching the exact schema:
            1. seoScore: An integer from 0 to 100 representing the overall SEO strength.
            2. audits: A list of exactly 4 audit items.
               Each item must have:
               - passed: A boolean (true if the metadata passes this specific check, false if it needs improvement).
               - message: A concise, actionable, and professional sentence explaining why it passed or what to fix.

            Evaluate based on the following 4 criteria:
            1. Title Optimization (Is it between 30-70 characters? Is it click-worthy and searchable?)
            2. Tag Relevancy & Synergy (Are there adequate tags? Do they match the title intent?)
            3. Description Structure (Does it have a strong hook in the first 2 sentences? Are there keyword variants?)
            4. Call To Action (Are there links or clear CTAs in the description?)
            """;

        String userPrompt = """
            Analyze this video metadata:
            Title: {title}
            Description: {desc}
            Tags: {tags}
            """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt)
                            .param("title", title)
                            .param("desc", description != null ? description : "None")
                            .param("tags", tags != null && !tags.isEmpty() ? String.join(", ", tags) : "None")
                    )
                    .call()
                    .entity(VideoAiAuditDto.class);
        } catch (Exception e) {
            log.error("Error invoking Gemini for video audit", e);
            
            // Fallback to basic heuristics if Gemini fails or rate limits
            int passCount = 0;
            List<AuditItem> fallbackAudits = new ArrayList<>();
            
            boolean titleLength = title != null && title.length() >= 20 && title.length() <= 70;
            if (titleLength) passCount++;
            fallbackAudits.add(new AuditItem(titleLength, titleLength ? "Title length is optimized." : "Title should be between 20-70 characters."));

            boolean hasTags = tags != null && !tags.isEmpty();
            if (hasTags) passCount++;
            fallbackAudits.add(new AuditItem(hasTags, hasTags ? "Video uses tags." : "No tags found. Add tags to improve reach."));

            boolean synergy = false;
            if (tags != null && !tags.isEmpty() && title != null) {
                String lowerTitle = title.toLowerCase();
                synergy = tags.stream().filter(java.util.Objects::nonNull).anyMatch(t -> lowerTitle.contains(t.toLowerCase()));
            }
            if (synergy) passCount++;
            fallbackAudits.add(new AuditItem(synergy, synergy ? "Title keywords found in tags." : "Include your main title keywords in your tags."));

            boolean hasLinks = description != null && (description.contains("http://") || description.contains("https://"));
            if (hasLinks) passCount++;
            fallbackAudits.add(new AuditItem(hasLinks, hasLinks ? "Description contains links (CTAs)." : "Add links to your description (Socials/Products)."));

            int score = passCount * 25;
            return new VideoAiAuditDto(score, fallbackAudits);
        }
    }
}
