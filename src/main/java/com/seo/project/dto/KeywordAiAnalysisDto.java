package com.seo.project.dto;

/**
 * KeywordAiAnalysisDto holds the structured AI-generated SEO analysis and suggestions for a search term.
 */
public record KeywordAiAnalysisDto(
    String difficulty,
    String competitionAdvice,
    String growthPotential,
    String seoAdvice
) {}
