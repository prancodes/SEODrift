package com.seo.project.dto;

import java.util.List;

/**
 * AiGenerationDto holds the structured generation output from Gemini.
 */
public record AiGenerationDto(
    List<String> titleSuggestions,
    String description,
    String hook,
    List<String> recommendedTags,
    List<String> recommendedHashtags,
    List<ChapterDto> chapters
) {
    /**
     * ChapterDto represents a timestamped video outline segment.
     */
    public record ChapterDto(
        String timestamp,
        String title
    ) {}
}
