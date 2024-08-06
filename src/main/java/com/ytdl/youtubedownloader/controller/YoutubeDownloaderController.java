package com.ytdl.youtubedownloader.controller;

import org.springframework.web.bind.annotation.RestController;

import com.ytdl.youtubedownloader.service.YoutubeDownloaderService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class YoutubeDownloaderController {
    
    @Autowired
    YoutubeDownloaderService youtubeDownloaderService;

   
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @PostMapping("/downloadVideo")
    public ResponseEntity<Resource> downloadVideo(@RequestBody Map<String, String> request) throws Exception {
        try {
            System.out.println("Request from frontend: "+request);
            return youtubeDownloaderService.downloadVideo(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Technical issue");
        }
       
    }
}
