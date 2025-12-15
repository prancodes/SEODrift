package com.seo.project.dto;

public class ThumbnailOptions {
    private String resolutionKey; // e.g., "720p", "480p"
    private String sizeInfo; // e.g., "1280x720"
    private String imageUrl; // The direct YouTube URL

    public ThumbnailOptions(String resolutionKey, String sizeInfo, String imageUrl) {
        this.resolutionKey = resolutionKey;
        this.sizeInfo = sizeInfo;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getResolutionKey() {
        return resolutionKey;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    
// do we need setters here?
// No, we do not need setters here as the fields are set via the constructor and are not intended to be modified afterwards.
}