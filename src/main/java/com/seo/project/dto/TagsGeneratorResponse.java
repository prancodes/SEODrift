package com.seo.project.dto;

import java.util.List;

public record TagsGeneratorResponse(
    VideoTagsInfo primaryVideo,
    List<VideoTagsInfo> relatedVideos
) {}