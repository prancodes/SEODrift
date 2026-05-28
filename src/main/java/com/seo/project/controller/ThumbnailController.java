package com.seo.project.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.seo.project.dto.ThumbnailOptions;
import com.seo.project.dto.ThumbnailRequest;
import com.seo.project.service.ThumbnailService;

@Controller
public class ThumbnailController {

    @Autowired
    private ThumbnailService service;

    // Endpoint to display the thumbnail input page
    @GetMapping("/thumbnail")
    public String showThumbnailPage(Model model) {
        model.addAttribute("thumbnailRequest", new ThumbnailRequest());
        return "thumbnail";
    }

    // Endpoint to process the thumbnail request
    @PostMapping("/thumbnail")
    public String fetchThumbnailData(@ModelAttribute("thumbnailRequest") ThumbnailRequest request, Model model) {

        String videoUrl = request.getUrl();
        String videoId = service.extractVideoId(videoUrl);

        if (videoId != null && !videoId.isEmpty()) {

            // Fetch Metadata (Title + Channel) for better filename during download
            Map<String,String> metadata = service.fetchVideoMetadata(videoId);

            String videoTitle = metadata.getOrDefault("title", "YouTube Video (" + videoId + ")");
            String channelName = metadata.getOrDefault("author_name", "YouTube Channel");

            model.addAttribute("videoTitle", videoTitle);
            model.addAttribute("channelName", channelName);

            // Prepare Thumbnail Options
            List<ThumbnailOptions> options = new ArrayList<>();

            // 1. Max Resolution (1280x720) - "1080p"
            options.add(new ThumbnailOptions("1080p (HD)", "1280x720", 
                "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg"));

            // 2. Standard Definition (720x480) - "720p"
            options.add(new ThumbnailOptions("720p (SD)", "720x480", 
                "https://img.youtube.com/vi/" + videoId + "/sddefault.jpg"));

            // 3. High Quality (480x360) - "480p"
            options.add(new ThumbnailOptions("480p", "480x360", 
                "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"));
                
            // 4. Medium Quality (320x180) - "320p"
            options.add(new ThumbnailOptions("320p", "320x180", 
                "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg"));

            // 5. Low Quality (120x90) - "Thumbnail"
            options.add(new ThumbnailOptions("(LQ)", "120x90", 
                "https://img.youtube.com/vi/" + videoId + "/default.jpg"));
                
            model.addAttribute("thumbnailOptions", options);
        } else {
            model.addAttribute("error", "Invalid YouTube URL. Please enter a valid URL.");
        }

        return "thumbnail";
    }

    // Endpoint to download the thumbnail image
    @GetMapping("/thumbnail/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadThumbnail(
        @RequestParam String imageUrl, 
        @RequestParam(defaultValue = "thumbnail") String title) {
        return service.downloadImage(imageUrl, title);
    }

}
