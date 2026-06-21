package com.seo.project.service;

import com.seo.project.dto.AiGenerationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

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
            String creatorChannelName) {
        
        log.info("Generating AI content workspace for topic: '{}', tone: '{}', user channel: '{}'", 
                topic, tone, creatorChannelName);

        String systemPrompt = """
            You are a senior YouTube SEO specialist, script writer, and metadata optimizer.
            Your task is to generate highly optimized, click-worthy, and algorithm-friendly metadata and structure for a video.
            
            Strict requirements:
            1. You MUST generate 3 title suggestions that are optimized for high Click-Through-Rate (CTR) and search indexability.
            2. The description must be highly engaging, start with a hook, include placeholders for links/Call-To-Actions (CTAs) in bracket format like [Insert Link Here], and naturally incorporate keyword variants. IMPORTANT: DO NOT include the timestamps or chapters in the description body. Use proper spacing, paragraph breaks (double newlines), and emojis to ensure the description is highly readable and visually appealing. Do not output a single wall of text.
            3. Provide a hook / intro script outline to grab attention in the first 15 seconds.
            4. Recommend exactly 15 to 20 relevant tags and 3 to 5 trending hashtags.
            5. Create an outline of recommended chapters (timestamp and title) based on standard pacing for the target topic. Provide these ONLY in the chapters JSON array.
            
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
                    .system(systemPrompt)
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
    public com.seo.project.dto.KeywordAiAnalysisDto analyzeKeyword(
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
                    .entity(com.seo.project.dto.KeywordAiAnalysisDto.class);
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
            
            return new com.seo.project.dto.KeywordAiAnalysisDto(
                    diff,
                    advice,
                    growthText,
                    fallbackSeo
            );
        }
    }
}
