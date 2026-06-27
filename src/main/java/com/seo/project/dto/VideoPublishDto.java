package com.seo.project.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoPublishDto {
    private String title;
    private String description;
    private String tags;
    private String privacyStatus;
    private String categoryId;
    private MultipartFile file;
    private String scheduledPublishTime;
}
