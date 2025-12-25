package com.seo.project.controller;

import com.seo.project.dto.VideoInfo;
import com.seo.project.dto.VideoLoadRequest;
import com.seo.project.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

@Controller
public class VideoController {

    @Autowired
    private VideoService videoService;

    @GetMapping("/video")
    public String showVideoPage(Model model) {
        model.addAttribute("videoRequest", new VideoLoadRequest());
        return "video";
    }

    @PostMapping("/video")
    public String fetchVideoOptions(@ModelAttribute("videoRequest") VideoLoadRequest request, Model model) {
        String url = request.getUrl();
        
        // Call Service which calls yt-dlp
        VideoInfo info = videoService.getVideoDetails(url);

        if (info != null && info.formats() != null && !info.formats().isEmpty()) {
            model.addAttribute("videoInfo", info);

            // Simple Logic: Top 2 formats are "Main", rest are "Other"
            if (info.formats().size() > 2) {
                model.addAttribute("mainFormats", info.formats().subList(0, 2));
                model.addAttribute("otherFormats", info.formats().subList(2, info.formats().size()));
            } else {
                model.addAttribute("mainFormats", info.formats());
            }
        } else {
            model.addAttribute("error", "Unable to fetch video. Please check the URL.");
        }
        return "video";
    }

    @GetMapping("/video/download")
    public ResponseEntity<StreamingResponseBody> downloadVideo(
            @RequestParam String videoUrl,
            @RequestParam String title,
            @RequestParam(required = false) String formatId) {

        // Fetch fresh video info to get latest download URLs (original URLs expire)
        VideoInfo info = videoService.getVideoDetails(videoUrl);
        if (info == null || info.formats().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Find the format the user selected
        String downloadUrl = null;
        String ext = "mp4";
        if (formatId != null && !formatId.isEmpty()) {
            for (com.seo.project.dto.VideoFormat fmt : info.formats()) {
                if (fmt.format().equals(formatId)) {
                    downloadUrl = fmt.downloadUrl();
                    ext = fmt.format().toLowerCase();
                    break;
                }
            }
        }

        // If no specific format found, use the first one
        if (downloadUrl == null && !info.formats().isEmpty()) {
            downloadUrl = info.formats().get(0).downloadUrl();
            ext = info.formats().get(0).format().toLowerCase();
        }

        if (downloadUrl == null) {
            return ResponseEntity.badRequest().build();
        }

        // Sanitize Filename
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filename = cleanTitle + "." + ext.toLowerCase();

        // Get Flux from WebClient
        Flux<DataBuffer> flux = videoService.streamVideo(downloadUrl, filename).getBody();

        if (flux == null) {
            return ResponseEntity.internalServerError().build();
        }

        // Bridge Flux to InputStream for Tomcat
        StreamingResponseBody stream = outputStream -> {
            DataBufferUtils.write(flux, outputStream)
                    .map(DataBufferUtils::release)
                    .blockLast();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }
}