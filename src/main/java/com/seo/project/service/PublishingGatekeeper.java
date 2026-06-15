package com.seo.project.service;

import com.seo.project.dto.VideoPublishDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublishingGatekeeper {

    public record GatekeeperResult(boolean passed, int score, List<String> warnings) {}

    public GatekeeperResult validate(VideoPublishDto dto) {
        List<String> warnings = new ArrayList<>();
        int score = 0;

        // Title validation
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            warnings.add("Title is missing.");
        } else {
            int len = dto.getTitle().length();
            if (len >= 30 && len <= 70) {
                score += 40;
            } else {
                warnings.add("Title length should be between 30 and 70 characters for optimal SEO.");
                score += 15;
            }
        }

        // Description validation
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            warnings.add("Description is missing.");
        } else {
            if (dto.getDescription().length() >= 250) {
                score += 30;
            } else {
                warnings.add("Description is too short. Aim for at least 250 characters.");
                score += 10;
            }
            if (!dto.getDescription().contains("http")) {
                warnings.add("Description should contain at least one external link or social media link.");
            } else {
                score += 10;
            }
            if (!dto.getDescription().contains("00:00")) {
                warnings.add("Consider adding video chapters starting with 00:00.");
            } else {
                score += 10;
            }
        }

        // Tags validation
        if (dto.getTags() == null || dto.getTags().trim().isEmpty()) {
            warnings.add("Tags are missing.");
        } else {
            String[] tagsArray = dto.getTags().split(",");
            if (tagsArray.length >= 5) {
                score += 10;
            } else {
                warnings.add("Use at least 5 relevant tags.");
                score += 5;
            }
        }

        // Gatekeeper threshold
        boolean passed = score >= 70;
        if (!passed) {
            warnings.add("Publishing Readiness Score is too low (" + score + "/100). Please optimize further before publishing.");
        }

        return new GatekeeperResult(passed, score, warnings);
    }
}
