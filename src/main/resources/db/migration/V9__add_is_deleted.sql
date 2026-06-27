-- V9__add_is_deleted.sql

ALTER TABLE video_analyses ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
