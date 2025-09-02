package com.mahmud.model;

public class DownloadProgress {
    private final double percentage;
    private final String speed;
    private final String eta;
    private final Long downloadedBytes;
    private final Long totalBytes;

    public DownloadProgress(double percentage, String speed, String eta, Long downloadedBytes, Long totalBytes) {
        this.percentage = percentage;
        this.speed = speed;
        this.eta = eta;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
    }

    public double getPercentage() { return percentage; }
    public String getSpeed() { return speed; }
    public String getEta() { return eta; }
    public Long getDownloadedBytes() { return downloadedBytes; }
    public Long getTotalBytes() { return totalBytes; }
}
