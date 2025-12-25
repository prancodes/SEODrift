package com.seo.project.dto;

public record VideoFormat(
    String quality,     // e.g. "720p", "1080p"
    String format,      // e.g. "MP4"
    String size,        // e.g. "124 MB"
    String downloadUrl, // The direct googlevideo link
    boolean hasAudio,   // To detect if it's a silent video (common for 1080p)
    boolean hasVideo    // To detect audio-only files
) {}