package com.seo.project.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple controller to handle public static pages.
 */
@Slf4j
@Controller
public class WebController {

    @GetMapping({"/", "/home"})
    public String homePage(Model model) {
        log.debug("Routing to Landing Page");
        model.addAttribute("metaDescription", "Master your YouTube strategy with SEODrift. Generate optimized SEO tags, download high-res thumbnails, audit channel health, and get deep video analytics.");
        model.addAttribute("metaKeywords", "youtube seo, youtube tag generator, thumbnail downloader, youtube analytics, channel audit, video seo, seodrift");
        model.addAttribute("ogTitle", "SEODrift - Master Your YouTube Strategy");
        return "index";
    }

    @GetMapping("/privacy")
    public String privacyPage(Model model) {
        log.debug("Routing to Privacy Policy Page");
        model.addAttribute("metaDescription", "Privacy Policy for SEODrift. Read about how we collect, protect, and use your personal information and Google/YouTube API data securely.");
        model.addAttribute("metaKeywords", "privacy policy, seodrift privacy, google oauth security, data protection");
        model.addAttribute("ogTitle", "Privacy Policy | SEODrift");
        return "privacy";
    }

    @GetMapping("/terms")
    public String termsPage(Model model) {
        log.debug("Routing to Terms of Service Page");
        model.addAttribute("metaDescription", "Terms of Service for SEODrift. Read our user terms, rules of conduct, and service guidelines for using our YouTube optimization tools.");
        model.addAttribute("metaKeywords", "terms of service, seodrift terms, user agreement");
        model.addAttribute("ogTitle", "Terms of Service | SEODrift");
        return "terms";
    }
}
