-- V4__private_metrics.sql
-- Add private YouTube Analytics metrics cache to users and history snapshots

ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_watch_time BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_impressions BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_ctr DOUBLE PRECISION DEFAULT 0.0;

ALTER TABLE user_channel_snapshots ADD COLUMN IF NOT EXISTS watch_time BIGINT;
ALTER TABLE user_channel_snapshots ADD COLUMN IF NOT EXISTS impressions BIGINT;
ALTER TABLE user_channel_snapshots ADD COLUMN IF NOT EXISTS ctr DOUBLE PRECISION;
