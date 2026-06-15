package com.seo.project.controller;

import com.seo.project.dto.VideoPublishDto;
import com.seo.project.service.PublishingGatekeeper;
import com.seo.project.service.YouTubePublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/publish")
@RequiredArgsConstructor
public class PublishController {

    private final YouTubePublishService publishService;
    private final PublishingGatekeeper gatekeeper;
    private final MultipartResolver multipartResolver;

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> publishVideo(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            HttpServletRequest request) {

        if (!multipartResolver.isMultipart(request)) {
             return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Request is not a valid multipart request."
            ));
        }

        MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);

        VideoPublishDto dto = new VideoPublishDto();
        dto.setTitle(multipartRequest.getParameter("title"));
        dto.setDescription(multipartRequest.getParameter("description"));
        dto.setTags(multipartRequest.getParameter("tags"));
        dto.setPrivacyStatus(multipartRequest.getParameter("privacyStatus"));
        dto.setCategoryId(multipartRequest.getParameter("categoryId"));
        dto.setFile(multipartRequest.getFile("file"));

        log.info("Received request to publish video: {}", dto.getTitle());

        if (dto.getFile() == null || dto.getFile().isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Video file is required."
            ));
        }

        // 1. Run Gatekeeper Validation
        PublishingGatekeeper.GatekeeperResult result = gatekeeper.validate(dto);
        if (!result.passed()) {
            log.warn("Gatekeeper validation failed for video: {}. Score: {}", dto.getTitle(), result.score());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "score", result.score(),
                    "warnings", result.warnings()
            ));
        }

        // 2. Upload to YouTube
        try {
            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            String videoId = publishService.uploadVideo(accessToken, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "videoId", videoId,
                    "score", result.score()
            ));
        } catch (Exception e) {
            log.error("Exception occurred during video upload", e);
            
            // Check if root cause is 401 Unauthorized from Google
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            if (rootCause instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException gex = (GoogleJsonResponseException) rootCause;
                if (gex.getStatusCode() == 401) {
                    log.warn("YouTube API returned 401. Triggering session re-authorization.");
                    throw new ClientAuthorizationRequiredException("google");
                }
            }

            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to upload video: " + e.getMessage()
            ));
        }
    }
}
