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
        command.add("-f");
        command.add(option.getFormatId());
        command.add("-o");
        command.add(new File(downloadPath, "%(title)s.%(ext)s").getAbsolutePath());
        
        if (useCookies && browserType != null) {
            command.add("--cookies-from-browser");
            command.add(browserType.getValue());
        }
        
        command.add("--progress");
        command.add(url);
        
        return command;
    }
}