package com.seo.project.dto;

import java.util.List;

public record VideoInfo(
    String title,
    String channel,
    String thumbnail,
    List<VideoFormat> formats // All available downloads
) {}