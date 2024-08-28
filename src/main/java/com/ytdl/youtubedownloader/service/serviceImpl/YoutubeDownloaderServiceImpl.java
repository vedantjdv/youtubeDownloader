package com.ytdl.youtubedownloader.service.serviceImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.ytdl.youtubedownloader.controller.YoutubeDownloaderController;
import com.ytdl.youtubedownloader.service.YoutubeDownloaderService;

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

        String command = "yt-dlp -f \"bestvideo[height<=" + videoQuality + "]+bestaudio/best[height<=" + videoQuality + "]\" -o \"" + uniqueFilePath + "\" " + videoUrl;
        System.out.println(command);
        try {
            Process process = createProcess(command);

            // Capture standard output and errors
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = outputReader.readLine()) != null) {
                    LOGGER.info("yt-dlp output: " + line);
                }
                while ((line = errorReader.readLine()) != null) {
                    LOGGER.warning("yt-dlp warning/error: " + line);
                }
            }
            System.out.println("72");
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.severe("yt-dlp command failed with exit code: " + exitCode);
                return ResponseEntity.status(500).build();
            }
            System.out.println("78");
            // Check if the file was created
            File videoFile = new File(uniqueFilePath);
            if (!videoFile.exists()) {
                LOGGER.severe("Video file was not created. File path: " + uniqueFilePath);
                return ResponseEntity.status(500).build();
            }
            System.out.println("85");
            // Stream the video file to the user's browser
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + videoFile.getName());
            Resource resource = new FileSystemResource(videoFile);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            LOGGER.warning("Unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(500).build();
        } finally {
            // Clean up the downloaded file
            new File(uniqueFilePath).delete();
        }
    }
    
    @Override
    public Process createProcess(String command) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", command).start();
        } else {
            return new ProcessBuilder("bash", "-c", command).start();
        }
    }
}
