package com.seo.project.controller;

import com.seo.project.dto.TagsGeneratorResponse;
import com.seo.project.service.TagsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TagsController {

    private final TagsService tagsService;

    public TagsController(TagsService tagsService) {
        this.tagsService = tagsService;
    }

    @GetMapping("/tags")
    public String showTagsPage() {
        return "tags";
    }

    @PostMapping("/tags")
    public String generateTags(@RequestParam("query") String query, Model model) {
        try {
            TagsGeneratorResponse response = tagsService.generateTags(query);
            
            if (response != null) {
                model.addAttribute("primary", response.primaryVideo());
                model.addAttribute("relatedVideos", response.relatedVideos());
                model.addAttribute("query", query); // Keep the input in the box
            } else {
                model.addAttribute("error", "No videos found for your input.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching tags: " + e.getMessage());
        }
        return "tags";
    }
}