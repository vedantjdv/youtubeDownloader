package com.ytdl.youtubedownloader.service.serviceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.ytdl.youtubedownloader.controller.YoutubeDownloaderController;
import com.ytdl.youtubedownloader.service.YoutubeDownloaderService;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class YoutubeDownloaderServiceImpl implements YoutubeDownloaderService {

    private static final Logger LOGGER = Logger.getLogger(YoutubeDownloaderController.class.getName());

    @Override
    public ResponseEntity<Resource> downloadVideo(@RequestBody Map<String, String> request) {
        String videoUrl = request.get("url");
        String videoQuality = request.get("videoQuality");
        if (videoUrl == null || videoUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        videoQuality = videoQuality.replace("p", "");
        String currentDir = System.getProperty("user.dir");

        // Extract video ID from URL
        String videoId = videoUrl.substring(videoUrl.indexOf("v=") + 2);

        // Get video title using yt-dlp
        String getTitleCommand = "yt-dlp --get-title " + videoUrl;
        String videoTitle = "";
        try {
            Process getTitleProcess = createProcess(getTitleCommand);
            BufferedReader titleReader = new BufferedReader(new InputStreamReader(getTitleProcess.getInputStream()));
            videoTitle = titleReader.readLine().replaceAll("[^a-zA-Z0-9-_\\.]", "_"); // Clean the title for filename
            getTitleProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Failed to get video title: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }

        // Create a unique file path
        String uniqueFilePath = currentDir + File.separator + videoTitle + "_" + videoId + "_" + videoQuality + "p.mp4";

        // Check if the file already exists
        File videoFile = new File(uniqueFilePath);
        if (videoFile.exists()) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + videoFile.getName());
            Resource resource = new FileSystemResource(videoFile);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }

        String command = "yt-dlp -f 'bestvideo[height<=" + videoQuality + "]+bestaudio/best[height<=" + videoQuality + "]' -o \"" + uniqueFilePath + "\" " + videoUrl;
        System.out.println(command);
        try {
            Process process = createProcess(command);

            // Capture standard output
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = outputReader.readLine()) != null) {
                    LOGGER.info("yt-dlp output: " + line);
                }
            }

            // Capture standard error
            boolean hasWarnings = false;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    LOGGER.warning("yt-dlp warning/error: " + line);
                    hasWarnings = true;
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.severe("yt-dlp command failed with exit code: " + exitCode);
                return ResponseEntity.status(500).build();
            }

            if (!videoFile.exists()) {
                LOGGER.severe("Video file was not created. File path: " + uniqueFilePath);
                return ResponseEntity.status(500).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + videoFile.getName());

            Resource resource = new FileSystemResource(videoFile);
            ResponseEntity<Resource> responseEntity = ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

            if (hasWarnings) {
                LOGGER.warning("The download completed with warnings. Please check the logs for details.");
                return ResponseEntity.status(206).body(resource); // 206 Partial Content to indicate warnings
            }

            return responseEntity;
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private Process createProcess(String command) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", command).start();
        } else {
            return new ProcessBuilder("bash", "-c", command).start();
        }
    }
}
