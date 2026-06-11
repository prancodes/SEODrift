package com.seo.project.controller;

import com.seo.project.dto.AiGenerationDto;
import com.seo.project.dto.TagsGeneratorResponse;
import com.seo.project.model.User;
import com.seo.project.model.VideoAnalysis;
import com.seo.project.repository.UserRepository;
import com.seo.project.repository.VideoAnalysisRepository;
import com.seo.project.service.AiWorkspaceService;
import com.seo.project.service.TagsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WorkspaceController handles routes and API requests for the AI Content Workspace,
 * enabling draft saving, keyword researching, and content optimization.
 */
@Slf4j
@Controller
public class WorkspaceController {

    private final UserRepository userRepository;
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final TagsService tagsService;
    private final AiWorkspaceService aiWorkspaceService;
    private final ObjectMapper objectMapper;

    public WorkspaceController(UserRepository userRepository,
                               VideoAnalysisRepository videoAnalysisRepository,
                               TagsService tagsService,
                               AiWorkspaceService aiWorkspaceService,
                               ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.tagsService = tagsService;
        this.aiWorkspaceService = aiWorkspaceService;
        this.objectMapper = objectMapper;
    }

    /**
     * Workspace draft data representation.
     */
    public record WorkspaceDraftDto(
        Long id,
        String topic,
        String tone,
        String title,
        String description,
        String hook,
        List<String> tags,
        List<String> hashtags,
        List<AiGenerationDto.ChapterDto> chapters,
        Integer seoScore
    ) {}

    /**
     * Generation request payload.
     */
    public record GenerationRequest(String topic, String tone) {}

    /**
     * Save draft request payload.
     */
    public record SaveDraftRequest(
        Long draftId,
        String topic,
        String tone,
        String title,
        String description,
        String hook,
        List<String> tags,
        List<String> hashtags,
        List<AiGenerationDto.ChapterDto> chapters,
        Integer seoScore
    ) {}

    /**
     * Renders the workspace HTML template.
     * Optionally loads a previously saved draft.
     */
    @GetMapping("/workspace")
    public String showWorkspacePage(
            @RequestParam(value = "draftId", required = false) Long draftId,
            Authentication authentication,
            Model model) {

        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("Unauthorized access attempt to /workspace. Redirecting to home.");
            return "redirect:/";
        }

        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/";
        }
        User user = userOpt.get();

        model.addAttribute("user", user);

        if (draftId != null) {
            Optional<VideoAnalysis> analysisOpt = videoAnalysisRepository.findById(draftId);
            if (analysisOpt.isPresent()) {
                VideoAnalysis analysis = analysisOpt.get();
                // Security check: Verify owner
                if (analysis.getUser().getId().equals(user.getId())) {
                    String urlField = analysis.getVideoUrl();
                    if (urlField != null && urlField.startsWith("ai-draft:")) {
                        try {
                            String jsonContent = urlField.substring("ai-draft:".length());
                            WorkspaceDraftDto draft = objectMapper.readValue(jsonContent, WorkspaceDraftDto.class);
                            // Set ID to match entity
                            draft = new WorkspaceDraftDto(
                                    analysis.getId(),
                                    draft.topic(),
                                    draft.tone(),
                                    draft.title(),
                                    draft.description(),
                                    draft.hook(),
                                    draft.tags(),
                                    draft.hashtags(),
                                    draft.chapters(),
                                    analysis.getSeoScore()
                            );
                            model.addAttribute("draft", draft);
                            model.addAttribute("draftTagsJson", objectMapper.writeValueAsString(draft.tags()));
                            model.addAttribute("draftHashtagsJson", objectMapper.writeValueAsString(draft.hashtags()));
                            model.addAttribute("draftChaptersJson", objectMapper.writeValueAsString(draft.chapters()));
                            log.info("Loaded workspace draft [{}] for user [{}]", draftId, email);
                        } catch (Exception e) {
                            log.error("Failed to parse saved draft JSON for ID {}: {}", draftId, e.getMessage());
                            model.addAttribute("error", "Failed to parse saved draft details.");
                            model.addAttribute("draftTagsJson", "[]");
                            model.addAttribute("draftHashtagsJson", "[]");
                            model.addAttribute("draftChaptersJson", "[]");
                        }
                    }
                } else {
                    log.warn("User {} tried to access draft ID {} owned by another user.", email, draftId);
                }
            }
        } else {
            model.addAttribute("draftTagsJson", "[]");
            model.addAttribute("draftHashtagsJson", "[]");
            model.addAttribute("draftChaptersJson", "[]");
        }

        return "workspace";
    }

    /**
     * Triggers AI content generation using competitor research data and Gemini.
     */
    @PostMapping("/api/workspace/generate")
    @ResponseBody
    public ResponseEntity<?> generateContent(
            @RequestBody GenerationRequest request,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        if (request.topic() == null || request.topic().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Topic is required"));
        }

        try {
            List<String> competitorTitles = new ArrayList<>();
            List<String> competitorTags = new ArrayList<>();

            // 1. Gather Search / Competitor context
            TagsGeneratorResponse tagsResponse = tagsService.generateTags(request.topic());
            if (tagsResponse != null) {
                if (tagsResponse.primaryVideo() != null) {
                    competitorTitles.add(tagsResponse.primaryVideo().title());
                    if (tagsResponse.primaryVideo().tags() != null) {
                        competitorTags.addAll(tagsResponse.primaryVideo().tags());
                    }
                }
                if (tagsResponse.relatedVideos() != null) {
                    tagsResponse.relatedVideos().forEach(v -> {
                        competitorTitles.add(v.title());
                        if (v.tags() != null) {
                            competitorTags.addAll(v.tags());
                        }
                    });
                }
            }

            // Clean lists
            List<String> cleanTitles = competitorTitles.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<String> cleanTags = competitorTags.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

            // 2. Call Gemini
            String channelTitle = user.getYoutubeChannelTitle() != null ? user.getYoutubeChannelTitle() : user.getName();
            AiGenerationDto generated = aiWorkspaceService.generateWorkspaceContent(
                    request.topic(),
                    request.tone(),
                    cleanTitles,
                    cleanTags,
                    channelTitle
            );

            return ResponseEntity.ok(generated);

        } catch (Exception e) {
            log.error("AI Generation failed for topic [{}]: {}", request.topic(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "AI Content generation failed: " + e.getMessage()));
        }
    }

    /**
     * Saves or updates an audited draft to the user's VideoAnalysis history log.
     */
    @PostMapping("/api/workspace/save")
    @ResponseBody
    public ResponseEntity<?> saveDraft(
            @RequestBody SaveDraftRequest request,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        try {
            // Serialize request payload to store inside the text column (except the draftId)
            WorkspaceDraftDto draftContent = new WorkspaceDraftDto(
                    null,
                    request.topic(),
                    request.tone(),
                    request.title(),
                    request.description(),
                    request.hook(),
                    request.tags(),
                    request.hashtags(),
                    request.chapters(),
                    request.seoScore()
            );
            String jsonPayload = objectMapper.writeValueAsString(draftContent);
            String videoUrlValue = "ai-draft:" + jsonPayload;

            VideoAnalysis analysis;
            if (request.draftId() != null) {
                // Update existing
                Optional<VideoAnalysis> existingOpt = videoAnalysisRepository.findById(request.draftId());
                if (existingOpt.isPresent()) {
                    analysis = existingOpt.get();
                    // Owner check
                    if (!analysis.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
                    }
                    analysis.setTitle(request.title());
                    analysis.setSeoScore(request.seoScore());
                    analysis.setVideoUrl(videoUrlValue);
                    analysis.setAnalyzedAt(LocalDateTime.now());
                } else {
                    return ResponseEntity.status(404).body(Map.of("error", "Draft not found"));
                }
            } else {
                // Create new
                String channelTitle = user.getYoutubeChannelTitle() != null ? user.getYoutubeChannelTitle() : user.getName();
                String thumbnail = user.getYoutubeAvatarUrl() != null && !user.getYoutubeAvatarUrl().isEmpty()
                        ? user.getYoutubeAvatarUrl()
                        : "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=256";

                analysis = VideoAnalysis.builder()
                        .videoId("draft-" + UUID.randomUUID().toString().substring(0, 8))
                        .title(request.title())
                        .channelTitle(channelTitle)
                        .thumbnailUrl(thumbnail)
                        .videoUrl(videoUrlValue)
                        .seoScore(request.seoScore())
                        .engagementRate(0.0)
                        .sentimentScore(0.0)
                        .user(user)
                        .analyzedAt(LocalDateTime.now())
                        .build();
            }

            VideoAnalysis saved = videoAnalysisRepository.save(analysis);
            log.info("Saved workspace draft ID [{}] for user [{}]", saved.getId(), email);

            return ResponseEntity.ok(Map.of("message", "Draft saved successfully", "draftId", saved.getId()));

        } catch (Exception e) {
            log.error("Failed to save draft for user [{}]: {}", email, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save draft: " + e.getMessage()));
        }
    }
}
