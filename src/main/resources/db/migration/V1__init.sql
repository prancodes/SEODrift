-- V1__init.sql
-- Database Schema for SEODrift Creator Intelligence Platform

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    google_id VARCHAR(255),
    picture_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Video Analyses (SEO Audits) table
CREATE TABLE IF NOT EXISTS video_analyses (
    id BIGSERIAL PRIMARY KEY,
    video_id VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    channel_title VARCHAR(255),
    thumbnail_url VARCHAR(500),
    video_url TEXT,
    seo_score INTEGER,
    engagement_rate DOUBLE PRECISION,
    sentiment_score DOUBLE PRECISION,
    user_id BIGINT,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_video_analyses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Competitor Channels table
CREATE TABLE IF NOT EXISTS competitor_channels (
    id BIGSERIAL PRIMARY KEY,
    channel_id VARCHAR(255) UNIQUE NOT NULL,
    custom_url VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    subscriber_count BIGINT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    video_count BIGINT DEFAULT 0,
    last_scraped_at TIMESTAMP
);

-- 4. User Competitor mapping (Many-to-Many association)
CREATE TABLE IF NOT EXISTS user_competitors (
    user_id BIGINT NOT NULL,
    competitor_channel_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, competitor_channel_id),
    CONSTRAINT fk_user_competitor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_competitor_channel FOREIGN KEY (competitor_channel_id) REFERENCES competitor_channels(id) ON DELETE CASCADE
);

-- 5. Competitor Snapshots (Time-series metrics)
CREATE TABLE IF NOT EXISTS competitor_snapshots (
    id BIGSERIAL PRIMARY KEY,
    competitor_channel_id BIGINT NOT NULL,
    subscriber_count BIGINT,
    view_count BIGINT,
    video_count BIGINT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshot_channel FOREIGN KEY (competitor_channel_id) REFERENCES competitor_channels(id) ON DELETE CASCADE
);

-- 6. Competitor Videos (To track uploads and detect changes)
CREATE TABLE IF NOT EXISTS competitor_videos (
    id BIGSERIAL PRIMARY KEY,
    competitor_channel_id BIGINT NOT NULL,
    video_id VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    CONSTRAINT fk_video_channel FOREIGN KEY (competitor_channel_id) REFERENCES competitor_channels(id) ON DELETE CASCADE
);

-- 7. Saved Keywords
CREATE TABLE IF NOT EXISTS saved_keywords (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_keyword UNIQUE (user_id, keyword),
    CONSTRAINT fk_saved_keywords_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 8. Keyword Trends (Time-series velocity engine)
CREATE TABLE IF NOT EXISTS keyword_trends (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    video_count_last_month INTEGER DEFAULT 0,
    video_count_this_month INTEGER DEFAULT 0,
    growth_rate DOUBLE PRECISION DEFAULT 0.0,
    recorded_date DATE NOT NULL,
    CONSTRAINT unique_keyword_date UNIQUE (keyword, recorded_date)
);

-- 9. Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
