package com.seo.project.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.seo.project.dto.VideoPublishDto;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.json.GoogleJsonError.ErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTubePublishService {

    public String uploadVideo(String accessToken, VideoPublishDto dto) throws Exception {
        log.info("Initializing YouTube Direct Publish Client via Google API Services...");
        
        YouTube youtubeService = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
        ).setApplicationName("SEODrift").build();

        Video video = new Video();

        // 1. Set Video Snippet (Metadata)
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(dto.getTitle());
        snippet.setDescription(dto.getDescription());
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().trim().isEmpty()) {
            snippet.setCategoryId(dto.getCategoryId());
        } else {
            snippet.setCategoryId("22"); // People & Blogs default
        }

        if (dto.getTags() != null && !dto.getTags().trim().isEmpty()) {
            List<String> tagsList = Arrays.stream(dto.getTags().split(","))
                    .map(s -> s.trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            snippet.setTags(tagsList);
        }
        video.setSnippet(snippet);

        // 2. Set Video Status (Privacy)
        VideoStatus status = new VideoStatus();
        
        if (dto.getScheduledPublishTime() != null && !dto.getScheduledPublishTime().trim().isEmpty()) {
            status.setPrivacyStatus("private");
            status.setPublishAt(new com.google.api.client.util.DateTime(dto.getScheduledPublishTime()));
        } else {
            status.setPrivacyStatus(dto.getPrivacyStatus() != null && !dto.getPrivacyStatus().isEmpty() 
                    ? dto.getPrivacyStatus().toLowerCase() : "private");
        }
        video.setStatus(status);

        // 3. Prepare media source and execute upload
        try (InputStream is = new BufferedInputStream(dto.getFile().getInputStream())) {
            InputStreamContent mediaContent = new InputStreamContent(
                    dto.getFile().getContentType() != null ? dto.getFile().getContentType() : "video/mp4",
                    is
            );
            mediaContent.setLength(dto.getFile().getSize());

            log.info("Executing YouTube Video Insert request. File size: {} bytes", dto.getFile().getSize());

            YouTube.Videos.Insert insertRequest = youtubeService.videos().insert(
                    List.of("snippet", "status"),
                    video,
                    mediaContent
            );

            // Enable resumable uploads for reliability
            insertRequest.getMediaHttpUploader().setDirectUploadEnabled(false);
            // Set chunk size to 5MB (must be multiple of 256KB)
            insertRequest.getMediaHttpUploader().setChunkSize(5 * 1024 * 1024);

            // Setup real-time server upload progress listener
            insertRequest.getMediaHttpUploader().setProgressListener(uploader -> {
                try {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            log.info("YouTube Upload Initiation Started...");
                            break;
                        case INITIATION_COMPLETE:
                            log.info("YouTube Upload Initiation Complete.");
                            break;
                        case MEDIA_IN_PROGRESS:
                            log.info("YouTube Upload in progress: {}% complete", (int) (uploader.getProgress() * 100));
                            break;
                        case MEDIA_COMPLETE:
                            log.info("YouTube Upload Complete!");
                            break;
                        case NOT_STARTED:
                            log.info("YouTube Upload not started.");
                            break;
                    }
                } catch (Exception ex) {
                    log.warn("Error getting upload progress: {}", ex.getMessage());
                }
            });

            log.info("Uploading video to YouTube...");
            try {
                Video returnedVideo = insertRequest.execute();
                if (returnedVideo != null && returnedVideo.getId() != null) {
                    log.info("Video successfully published to YouTube! Video ID: {}", returnedVideo.getId());
                    return returnedVideo.getId();
                } else {
                    throw new RuntimeException("Upload completed, but YouTube API did not return a Video ID.");
                }
            } catch (GoogleJsonResponseException e) {
                log.error("Google API Exception during upload: status={}, message={}", e.getStatusCode(), e.getMessage(), e);
                if (e.getDetails() != null && e.getDetails().getErrors() != null) {
                    for (ErrorInfo error : e.getDetails().getErrors()) {
                        if ("channelNotFound".equalsIgnoreCase(error.getReason())) {
                            throw new RuntimeException("No associated YouTube channel found for your Google account. Please visit YouTube.com, create a channel, and try again.");
                        }
                    }
                }
                throw new RuntimeException("YouTube API Error: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Generic Exception during upload", e);
                throw e;
            }
        }
    }
}
