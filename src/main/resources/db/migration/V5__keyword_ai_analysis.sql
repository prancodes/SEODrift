-- V5__keyword_ai_analysis.sql
-- Add columns to cache Gemini AI analysis results for search terms in the database

ALTER TABLE keyword_trends
ADD COLUMN IF NOT EXISTS ai_difficulty VARCHAR(50),
ADD COLUMN IF NOT EXISTS ai_competition_advice TEXT,
ADD COLUMN IF NOT EXISTS ai_growth_potential VARCHAR(150),
ADD COLUMN IF NOT EXISTS ai_seo_advice TEXT;
