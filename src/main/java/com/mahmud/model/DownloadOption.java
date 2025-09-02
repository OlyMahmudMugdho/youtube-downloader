package com.mahmud.model;

public class DownloadOption {
    private final String formatId;
    private final String extension;
    private final String resolution;
    private final String filesize;
    private final String description;
    
    public DownloadOption(String formatId, String extension, String resolution, 
                         String filesize, String description) {
        this.formatId = formatId;
        this.extension = extension;
        this.resolution = resolution;
        this.filesize = filesize;
        this.description = description;
    }
    
    public String getFormatId() { return formatId; }
    public String getExtension() { return extension; }
    public String getResolution() { return resolution; }
    public String getFilesize() { return filesize; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", description, resolution, 
                           filesize != null ? filesize : "Unknown size");
    }
}