-- V2__user_channel_details.sql
-- Extension to store user YouTube channel snapshot caching & history for the Analytics Console

ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_channel_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_channel_title VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_custom_url VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_uploads_playlist_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_subscriber_count BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_view_count BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_video_count BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_last_updated_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS user_channel_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscriber_count BIGINT,
    view_count BIGINT,
    video_count BIGINT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_snapshot_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
