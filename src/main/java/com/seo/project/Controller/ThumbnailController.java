package com.seo.project.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.seo.project.dto.ThumbnailOptions;
import com.seo.project.dto.ThumbnailRequest;
import com.seo.project.Service.ThumbnailService;

@Controller
public class ThumbnailController {

    @Autowired
    private ThumbnailService service;

    @GetMapping("/thumbnail")
    public String showThumbnailPage(Model model) {
        model.addAttribute("thumbnailRequest", new ThumbnailRequest());
        return "thumbnail";
    }

    @PostMapping("/thumbnail")
    public String fetchThumbnailData(@ModelAttribute("thumbnailRequest") ThumbnailRequest request, Model model) {

        String videoUrl = request.getUrl();
        String videoId = service.extractVideoId(videoUrl);

        if (videoId != null && !videoId.isEmpty()) {
            List<ThumbnailOptions> options = new ArrayList<>();

            // 1. Max Resolution (1280x720) - "1080p"
            options.add(new ThumbnailOptions("1080p (HD)", "1280x720", 
                "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg"));

            // 2. Standard Definition (640x480) - "720p"
            options.add(new ThumbnailOptions("720p (SD)", "640x480", 
                "https://img.youtube.com/vi/" + videoId + "/sddefault.jpg"));

            // 3. High Quality (480x360) - "480p"
            options.add(new ThumbnailOptions("480p", "480x360", 
                "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"));
                
            // 4. Medium Quality (320x180) - "320p"
            options.add(new ThumbnailOptions("320p", "320x180", 
                "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg"));

            // 5. Default (120x90) - "Thumbnail"
            options.add(new ThumbnailOptions("Default", "120x90", 
                "https://img.youtube.com/vi/" + videoId + "/default.jpg"));
                
            model.addAttribute("thumbnailOptions", options);
        } else {
            model.addAttribute("error", "Invalid YouTube URL. Please enter a valid URL.");
        }

        return "thumbnail";
    }

}
