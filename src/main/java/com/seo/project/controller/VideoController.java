package com.seo.project.controller;

import com.seo.project.dto.VideoInfo;
import com.seo.project.dto.VideoFormat;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        VideoInfo info = videoService.getVideoDetails(request.getUrl());
        if (info != null && info.formats() != null && !info.formats().isEmpty()) {
            model.addAttribute("videoInfo", info);
            
            List<VideoFormat> all = info.formats();
            List<VideoFormat> uniqueVideos = deduplicateVideos(all);
            uniqueVideos.sort((v1, v2) -> Integer.compare(parseResolution(v2.quality()), parseResolution(v1.quality())));

            VideoFormat card1 = !uniqueVideos.isEmpty() ? uniqueVideos.get(0) : null;
            VideoFormat card2 = uniqueVideos.size() > 1 ? uniqueVideos.get(1) : null;
            List<VideoFormat> otherVideos = uniqueVideos.size() > 2 ? uniqueVideos.subList(2, uniqueVideos.size()) : new ArrayList<>();
            List<VideoFormat> audioFormats = deduplicateAudio(all);

            model.addAttribute("card1", card1);
            model.addAttribute("card2", card2);
            model.addAttribute("videoFormats", otherVideos);
            model.addAttribute("audioFormats", audioFormats);
        } else {
            model.addAttribute("error", "Unable to fetch video.");
        }
        return "video";
    }

    // --- ASYNC API ENDPOINTS ---

    @GetMapping("/video/process")
    @ResponseBody
    public ResponseEntity<String> startProcess(
            @RequestParam String videoUrl,
            @RequestParam String formatId,
            @RequestParam String downloadToken,
            @RequestParam(required = false) String codec) {
        
        boolean isAudio = "audio".equalsIgnoreCase(codec);
        String ext;
        
        if (isAudio) {
            ext = "m4a"; 
        } else {
            ext = (codec != null && (codec.contains("avc") || codec.contains("h264"))) ? "mp4" : "webm";
        }
        
        videoService.startMergeTask(downloadToken, videoUrl, formatId, ext, isAudio);
        return ResponseEntity.ok("STARTED");
    }

    @GetMapping("/video/status")
    @ResponseBody
    public ResponseEntity<String> checkStatus(@RequestParam String downloadToken) {
        return ResponseEntity.ok(videoService.getTaskStatus(downloadToken));
    }

    @GetMapping("/video/serve")
    public ResponseEntity<StreamingResponseBody> serveFile(
            @RequestParam String downloadToken, 
            @RequestParam String title) {
        
        VideoService.DownloadTask task = videoService.getTask(downloadToken);
        if (task == null || !task.status.equals("COMPLETED")) {
            return ResponseEntity.badRequest().build();
        }

        String cleanTitle = title.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        
        // ✅ Add Suffix for Audio Files
        if (task.isAudio) {
            cleanTitle += " (Audio)";
        }

        String filename = cleanTitle + "." + getExt(task.filePath.toString());

        StreamingResponseBody stream = outputStream -> {
            try (InputStream fis = new FileInputStream(task.filePath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                Files.deleteIfExists(task.filePath);
                videoService.removeTask(downloadToken);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    // --- DIRECT DOWNLOAD (Backup/Direct Link) ---
    @GetMapping("/video/download")
    public ResponseEntity<StreamingResponseBody> downloadDirect(
            @RequestParam String videoUrl, @RequestParam String title, @RequestParam String formatId) {
        
        VideoInfo info = videoService.getVideoDetails(videoUrl);
        VideoFormat selected = info.formats().stream()
                .filter(f -> f.id().equals(formatId)).findFirst().orElse(info.formats().get(0));
        
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        if (!selected.hasVideo() && selected.hasAudio()) cleanTitle += "_(Audio)";
        
        String filename = cleanTitle + "." + selected.format().toLowerCase();
        Flux<DataBuffer> flux = videoService.streamVideo(selected.downloadUrl(), filename, selected.httpHeaders()).getBody();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(os -> DataBufferUtils.write(flux, os).map(DataBufferUtils::release).blockLast());
    }

    private String getExt(String path) { return path.substring(path.lastIndexOf(".") + 1); }
    private int parseResolution(String q) { try { Matcher m = Pattern.compile("(\\d+)").matcher(q); return m.find() ? Integer.parseInt(m.group(1)) : 0; } catch (Exception e) { return 0; } }
    
    private List<VideoFormat> deduplicateVideos(List<VideoFormat> all) {
        List<VideoFormat> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<VideoFormat> sortedPref = new ArrayList<>(all);
        sortedPref.sort((a, b) -> Boolean.compare(b.format().equalsIgnoreCase("mp4"), a.format().equalsIgnoreCase("mp4")));
        for (VideoFormat f : sortedPref) {
            if (!f.hasVideo()) continue;
            String key = f.quality(); 
            if (!seen.contains(key)) { unique.add(f); seen.add(key); }
        }
        return unique;
    }

    private List<VideoFormat> deduplicateAudio(List<VideoFormat> all) {
        List<VideoFormat> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (VideoFormat f : all) {
            if (f.hasVideo() || !f.hasAudio()) continue;
            String key = f.quality().split(",")[0].trim() + "-" + f.format();
            if (!seen.contains(key)) { unique.add(f); seen.add(key); }
        }
        return unique;
    }
}