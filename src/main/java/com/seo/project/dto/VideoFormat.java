package com.seo.project.dto;

import java.util.Map;

public record VideoFormat(
    String id,
    String quality,
    String format,
    String size,
    String downloadUrl,
    boolean hasAudio,
    boolean hasVideo,
    String videoCodec, // ✅ Added to detect avc1 vs vp9
    Map<String, String> httpHeaders
) {}