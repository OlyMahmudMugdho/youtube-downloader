package com.mahmud.service;

import com.mahmud.model.BrowserType;
import com.mahmud.model.DownloadOption;
import com.mahmud.util.ProcessUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YtDlpService {
    
    // Pattern to match format lines from yt-dlp --list-formats output
    private static final Pattern FORMAT_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+([^\\s]+(?:\\s+[^\\s]+)*)\\s*$"
    );
    
    public List<DownloadOption> getAvailableFormats(String url, boolean useCookies, 
                                                   BrowserType browserType) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        command.add("--list-formats");
        
        if (useCookies && browserType != null) {
            command.add("--cookies-from-browser");
            command.add(browserType.getValue());
        }
        
        command.add(url);
        
        String output = ProcessUtils.executeCommand(command);
        return parseFormats(output);
    }
    
    private List<DownloadOption> parseFormats(String output) {
        List<DownloadOption> options = new ArrayList<>();
        
        String[] lines = output.split("\n");
        boolean formatSectionStarted = false;
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines
            if (line.isEmpty()) continue;
            
            // Look for the format section header
            if (line.contains("format code") && line.contains("extension")) {
                formatSectionStarted = true;
                continue;
            }
            
            // Skip lines before format section
            if (!formatSectionStarted) continue;
            
            // Skip separator lines
            if (line.startsWith("-") || line.startsWith("=")) continue;
            
            // Parse format line
            DownloadOption option = parseFormatLine(line);
            if (option != null) {
                options.add(option);
            }
        }
        
        // Add some common combined formats if not present
        addCommonCombinedFormats(options);
        
        return options;
    }
    
    private DownloadOption parseFormatLine(String line) {
        // Split by whitespace, but preserve quoted strings
        String[] parts = line.split("\\s+");
        
        if (parts.length < 3) return null;
        
        try {
            String formatId = parts[0];
            String extension = parts[1];
            
            // Skip if format ID is not valid
            if (formatId.equals("ID") || formatId.equals("format")) return null;
            
            // Build description from remaining parts
            StringBuilder descBuilder = new StringBuilder();
            String resolution = "unknown";
            String filesize = null;
            
            for (int i = 2; i < parts.length; i++) {
                String part = parts[i];
                
                // Try to identify resolution
                if (part.matches("\\d+x\\d+") || part.matches("\\d+p")) {
                    resolution = part;
                } else if (part.matches("\\d+\\.\\d+[KMGT]?iB") || part.matches("\\d+\\.\\d+[KMGT]?B")) {
                    filesize = part;
                } else if (part.equals("audio") && i + 1 < parts.length && parts[i + 1].equals("only")) {
                    resolution = "audio only";
                    i++; // skip "only"
                }
                
                descBuilder.append(part).append(" ");
            }
            
            String description = descBuilder.toString().trim();
            if (description.isEmpty()) {
                description = formatId + " (" + extension + ")";
            }
            
            return new DownloadOption(formatId, extension, resolution, filesize, description);
            
        } catch (Exception e) {
            // If parsing fails, skip this line
            return null;
        }
    }
    
    private void addCommonCombinedFormats(List<DownloadOption> options) {
        // Add best quality option
        options.add(0, new DownloadOption("best", "mp4", "best available", null, 
            "Best quality (video+audio)"));
        
        // Add worst quality option
        options.add(new DownloadOption("worst", "mp4", "worst available", null, 
            "Worst quality (video+audio)"));
        
        // Add audio-only best
        options.add(new DownloadOption("bestaudio", "m4a", "audio only", null, 
            "Best audio quality"));
        
        // Add some common combined formats
        options.add(new DownloadOption("best[height<=720]", "mp4", "720p or lower", null, 
            "Best quality up to 720p"));
        
        options.add(new DownloadOption("best[height<=480]", "mp4", "480p or lower", null, 
            "Best quality up to 480p"));
    }
    
    public String getVideoTitle(String url, boolean useCookies, BrowserType browserType) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        command.add("--get-title");
        
        if (useCookies && browserType != null) {
            command.add("--cookies-from-browser");
            command.add(browserType.getValue());
        }
        
        command.add(url);
        
        String output = ProcessUtils.executeCommand(command);
        return output.trim();
    }
}