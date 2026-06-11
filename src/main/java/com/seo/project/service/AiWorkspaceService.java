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
}
