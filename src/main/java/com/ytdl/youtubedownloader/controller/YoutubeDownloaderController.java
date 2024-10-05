package com.ytdl.youtubedownloader.controller;

import org.springframework.web.bind.annotation.RestController;

import com.ytdl.youtubedownloader.service.YoutubeDownloaderService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class YoutubeDownloaderController {
    
    @Autowired
    YoutubeDownloaderService youtubeDownloaderService;

    private static final Logger LOGGER = Logger.getLogger(YoutubeDownloaderController.class.getName());
   
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @PostMapping("/downloadVideo")
    public ResponseEntity<Resource> downloadVideo(@RequestBody Map<String, String> request) throws Exception {
        try {
            System.out.println("Request from frontend: "+request);
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
            Process getTitleProcess = youtubeDownloaderService.createProcess(getTitleCommand);
            BufferedReader titleReader = new BufferedReader(new InputStreamReader(getTitleProcess.getInputStream()));
            videoTitle = titleReader.readLine().replaceAll("[^a-zA-Z0-9-_\\.]", "_"); // Clean the title for filename
            getTitleProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Failed to get video title: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }

        // Create a unique file path
        String uniqueFilePath = currentDir + File.separator + videoTitle + "_" + videoId + "_" + videoQuality + "p";

        String command = "yt-dlp -f \"bestvideo[height<=" + videoQuality + "]+bestaudio/best[height<=" + videoQuality + "]\" -o \"" + uniqueFilePath + "\" " + videoUrl;
        System.out.println(command);
        try {
            Process process = youtubeDownloaderService.createProcess(command);

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

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.severe("yt-dlp command failed with exit code: " + exitCode);
                return ResponseEntity.status(500).build();
            }

            // Check if the file was created
            File videoFile = new File(uniqueFilePath+".webm");
            System.out.println(uniqueFilePath);
            if (!videoFile.exists()) {
                LOGGER.severe("Video file was not created. File path: " + uniqueFilePath);
                return ResponseEntity.status(500).build();
            }

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
        }
            // return youtubeDownloaderService.downloadVideo(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Technical issue");
        }
       
    }

    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @GetMapping("/testDownload")
    public ResponseEntity<Resource> testDownload() {
        try {
            System.out.println("Entry in test Download");
            // Path to your video file
            Path videoPath = Paths.get("C:\\Other Projects\\youtubedownloader\\2_Second_Video_TK4N5W22Gts_1080p.mp4.webm"); // Adjust the path to your video
            System.out.println(videoPath);
            if (Files.exists(videoPath)) {
                File videoFile = videoPath.toFile();
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + videoFile.getName());
                Resource resource = new FileSystemResource(videoFile);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(videoFile.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
