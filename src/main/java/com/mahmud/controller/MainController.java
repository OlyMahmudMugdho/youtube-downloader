package com.mahmud.controller;

import com.mahmud.model.BrowserType;
import com.mahmud.model.DownloadOption;
import com.mahmud.model.DownloadProgress;
import com.mahmud.service.DownloadService;
import com.mahmud.service.YtDlpService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    
    @FXML private TextField urlField;
    @FXML private CheckBox useCookiesCheckBox;
    @FXML private ComboBox<BrowserType> browserComboBox;
    @FXML private Button fetchButton;
    @FXML private VBox formatOptionsContainer;
    @FXML private TextField downloadPathField;
    @FXML private Button browseButton;
    @FXML private Button downloadButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea logArea;
    
    private final YtDlpService ytDlpService = new YtDlpService();
    private final DownloadService downloadService = new DownloadService();
    private final List<CheckBox> formatCheckBoxes = new ArrayList<>();
    private List<DownloadOption> availableFormats = new ArrayList<>();
    private Task<Void> currentDownloadTask;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }
    
    private void setupUI() {
        // Initialize browser combo box
        browserComboBox.getItems().addAll(BrowserType.values());
        browserComboBox.setValue(BrowserType.CHROME);
        browserComboBox.setDisable(true);
        
        // Set default download path
        downloadPathField.setText(System.getProperty("user.home") + File.separator + "Downloads");
        
        // Initially disable download button
        downloadButton.setDisable(true);
        
        // Setup progress bar
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);
    }
    
    private void setupEventHandlers() {
        // Enable/disable browser selection based on cookies checkbox
        useCookiesCheckBox.setOnAction(e -> 
            browserComboBox.setDisable(!useCookiesCheckBox.isSelected()));
        
        // Fetch button action
        fetchButton.setOnAction(e -> fetchFormats());
        
        // Browse button action
        browseButton.setOnAction(e -> browseDownloadLocation());
        
        // Download button action
        downloadButton.setOnAction(e -> startDownload());
        
        // URL field enter key
        urlField.setOnAction(e -> fetchFormats());
    }
    
    @FXML
    private void fetchFormats() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Error", "Please enter a valid YouTube URL");
            return;
        }

        // Instead of querying yt-dlp for available formats, provide common presets
        fetchButton.setDisable(true);
        statusLabel.setText("Preparing formats...");
        formatOptionsContainer.getChildren().clear();
        formatCheckBoxes.clear();
        downloadButton.setDisable(true);

        List<DownloadOption> presets = new ArrayList<>();
        presets.add(new DownloadOption("best[height<=360]", "mp4", "360p", null, "360p (video+audio)"));
        presets.add(new DownloadOption("best[height<=480]", "mp4", "480p", null, "480p (video+audio)"));
        presets.add(new DownloadOption("best[height<=720]", "mp4", "720p", null, "720p (video+audio)"));
        presets.add(new DownloadOption("best[height<=1080]", "mp4", "1080p", null, "1080p (video+audio)"));

        availableFormats = presets;
        displayFormats(availableFormats);
        fetchButton.setDisable(false);
        statusLabel.setText("Formats ready");
    }
    
    private void displayFormats(List<DownloadOption> formats) {
        formatCheckBoxes.clear();
        formatOptionsContainer.getChildren().clear();
        
        if (formats.isEmpty()) {
            Label noFormatsLabel = new Label("No formats available");
            formatOptionsContainer.getChildren().add(noFormatsLabel);
            return;
        }
        
        // Group formats by type (video/audio)
        List<DownloadOption> videoFormats = new ArrayList<>();
        List<DownloadOption> audioFormats = new ArrayList<>();
        
        for (DownloadOption format : formats) {
            if (format.getResolution().equals("audio only")) {
                audioFormats.add(format);
            } else {
                videoFormats.add(format);
            }
        }
        
        // Add video formats
        if (!videoFormats.isEmpty()) {
            Label videoLabel = new Label("Video Formats:");
            videoLabel.setStyle("-fx-font-weight: bold;");
            formatOptionsContainer.getChildren().add(videoLabel);
            
            for (DownloadOption format : videoFormats) {
                CheckBox checkBox = new CheckBox(format.toString());
                checkBox.setUserData(format);
                formatCheckBoxes.add(checkBox);
                formatOptionsContainer.getChildren().add(checkBox);
            }
        }
        
        // Add audio formats
        if (!audioFormats.isEmpty()) {
            Label audioLabel = new Label("Audio Formats:");
            audioLabel.setStyle("-fx-font-weight: bold;");
            formatOptionsContainer.getChildren().add(audioLabel);
            
            for (DownloadOption format : audioFormats) {
                CheckBox checkBox = new CheckBox(format.toString());
                checkBox.setUserData(format);
                formatCheckBoxes.add(checkBox);
                formatOptionsContainer.getChildren().add(checkBox);
            }
        }
        
        // Enable download button when formats are available
        downloadButton.setDisable(false);
        
        // Add listener to checkboxes to ensure only one is selected
        for (CheckBox checkBox : formatCheckBoxes) {
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    // Uncheck all other checkboxes
                    for (CheckBox otherCheckBox : formatCheckBoxes) {
                        if (otherCheckBox != checkBox) {
                            otherCheckBox.setSelected(false);
                        }
                    }
                }
            });
        }
    }
    
    @FXML
    private void browseDownloadLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Location");
        
        // Set initial directory
        String currentPath = downloadPathField.getText();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(browseButton.getScene().getWindow());
        if (selectedDirectory != null) {
            downloadPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    @FXML
    private void startDownload() {
        // Get selected format
        DownloadOption selectedFormat = null;
        for (CheckBox checkBox : formatCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedFormat = (DownloadOption) checkBox.getUserData();
                break;
            }
        }
        
        if (selectedFormat == null) {
            showAlert("Error", "Please select a format to download");
            return;
        }
        
        String downloadPath = downloadPathField.getText().trim();
        if (downloadPath.isEmpty()) {
            showAlert("Error", "Please select a download location");
            return;
        }
        
        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists() || !downloadDir.isDirectory()) {
            showAlert("Error", "Invalid download location");
            return;
        }
        
        // Disable UI elements during download
        setUIEnabled(false);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("Starting download...");
        logArea.clear();
        
        // Start download task
        currentDownloadTask = downloadService.downloadVideo(
            urlField.getText().trim(),
            selectedFormat,
            downloadPath,
            useCookiesCheckBox.isSelected(),
            useCookiesCheckBox.isSelected() ? browserComboBox.getValue() : null,
            this::updateProgress,
            this::updateStatus
        );
        
        currentDownloadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            setUIEnabled(true);
            progressBar.setProgress(1.0);
            progressLabel.setText("Download completed!");
            statusLabel.setText("Download completed successfully");
            showAlert("Success", "Video downloaded successfully!");
        }));
        
        currentDownloadTask.setOnFailed(e -> Platform.runLater(() -> {
            setUIEnabled(true);
            progressBar.setProgress(0);
            progressLabel.setText("Download failed");
            statusLabel.setText("Download failed");
            Throwable exception = currentDownloadTask.getException();
            showAlert("Error", "Download failed: " + 
                     (exception != null ? exception.getMessage() : "Unknown error"));
        }));
        
        currentDownloadTask.setOnCancelled(e -> Platform.runLater(() -> {
            setUIEnabled(true);
            progressBar.setProgress(0);
            progressLabel.setText("Download cancelled");
            statusLabel.setText("Download cancelled");
        }));
        
        new Thread(currentDownloadTask).start();
    }
    
    private void updateProgress(DownloadProgress progress) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress.getPercentage() / 100.0);
            progressLabel.setText(String.format("%.1f%% - %s - ETA: %s", 
                progress.getPercentage(), progress.getSpeed(), progress.getEta()));
        });
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            logArea.appendText(status + "\n");
        });
    }
    
    private void setUIEnabled(boolean enabled) {
        urlField.setDisable(!enabled);
        useCookiesCheckBox.setDisable(!enabled);
        browserComboBox.setDisable(!enabled || !useCookiesCheckBox.isSelected());
        fetchButton.setDisable(!enabled);
        downloadPathField.setDisable(!enabled);
        browseButton.setDisable(!enabled);
        downloadButton.setDisable(!enabled);
        
        for (CheckBox checkBox : formatCheckBoxes) {
            checkBox.setDisable(!enabled);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void cancelDownload() {
        if (currentDownloadTask != null && currentDownloadTask.isRunning()) {
            currentDownloadTask.cancel();
        }
    }
}