package com.seo.project.dto;

import java.util.List;

// DTO to hold video tags information
public record VideoTagsInfo(
    String videoId,
    String title,
    String channelName,
    String thumbnailUrl,
    List<String> tags
) {}