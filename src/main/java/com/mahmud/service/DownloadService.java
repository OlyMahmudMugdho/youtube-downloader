package com.mahmud.service;

import com.mahmud.model.BrowserType;
import com.mahmud.model.DownloadOption;
import com.mahmud.model.DownloadProgress;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadService {
    private static final Pattern PROGRESS_PATTERN = 
        Pattern.compile("download\\s+(\\d+\\.\\d+)%.*?at\\s+([\\d\\.]+\\w+/s).*?ETA\\s+(\\d+:\\d+)");
    
    public Task<Void> downloadVideo(String url, DownloadOption option, String downloadPath,
                                   boolean useCookies, BrowserType browserType,
                                   Consumer<DownloadProgress> progressCallback,
                                   Consumer<String> statusCallback) {
        
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<String> command = buildDownloadCommand(url, option, downloadPath, 
                                                           useCookies, browserType);
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null && !isCancelled()) {
                        final String currentLine = line;
                        
                        // Parse progress
                        Matcher matcher = PROGRESS_PATTERN.matcher(line);
                        if (matcher.find()) {
                            double percentage = Double.parseDouble(matcher.group(1));
                            String speed = matcher.group(2);
                            String eta = matcher.group(3);
                            
                            javafx.application.Platform.runLater(() -> {
                                progressCallback.accept(new DownloadProgress(
                                    percentage, speed, eta, null, null));
                            });
                        }
                        
                        // Update status
                        javafx.application.Platform.runLater(() -> {
                            statusCallback.accept(currentLine);
                        });
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0 && !isCancelled()) {
                    throw new RuntimeException("Download failed with exit code: " + exitCode);
                }
                
                return null;
            }
        };
    }
    
    private List<String> buildDownloadCommand(String url, DownloadOption option, 
                                            String downloadPath, boolean useCookies, 
                                            BrowserType browserType) {
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        // If option requests a max height (e.g. best[height<=1080] or bestvideo[height<=1080])
        // convert to a sorting preference using -S so yt-dlp will prefer the target resolution.
        String fmt = option.getFormatId();
    if (fmt != null && fmt.contains("height<=")) {
            // extract number
            try {
                int idx = fmt.indexOf("height<=") + "height<=".length();
                String rest = fmt.substring(idx).replaceAll("[^0-9].*$", "");
                int target = Integer.parseInt(rest);
                // prefer exact resolution and higher fps
                command.add("-S");
                command.add("res:" + target + ",fps");
            } catch (Exception ignored) {
                // fall back to no sorting
            }
        }

    // Decide how to download based on the format id and option type
    // Be careful: descriptions like "360p (video+audio)" contain the word "audio" but are not audio-only.
    boolean isAudioOnly = (option.getResolution() != null && option.getResolution().equalsIgnoreCase("audio only"))
        || (option.getDescription() != null && option.getDescription().toLowerCase().contains("audio only"))
        || (fmt != null && (fmt.startsWith("bestaudio") || fmt.equalsIgnoreCase("bestaudio")));

        if (isAudioOnly) {
            // Audio-only: request best audio and extract to mp3
            command.add("-f");
            command.add(fmt != null && !fmt.isEmpty() ? fmt : "bestaudio");
            // extract audio and convert to mp3 so extension matches
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
            // best quality for audio
            command.add("--audio-quality");
            command.add("0");
        } else if (fmt != null && fmt.startsWith("bestvideo")) {
            // Video-only: download video stream only (no audio)
            command.add("-f");
            command.add(fmt);
            // when only video is downloaded, output may be e.g. webm; let user handle postprocessing
        } else {
            // Default (video+audio): prefer best video + best audio and merge into mp4
            command.add("-f");
            command.add("bestvideo+bestaudio/best");
            command.add("--merge-output-format");
            command.add("mp4");
        }
        command.add("-o");
        command.add(new File(downloadPath, "%(title)s.%(ext)s").getAbsolutePath());
        
    if (useCookies && browserType != null) {
            command.add("--cookies-from-browser");
            command.add(browserType.getValue());
        }
        
    // Ensure progress lines are emitted
    command.add("--newline");

    command.add(url);
        
        return command;
    }
}