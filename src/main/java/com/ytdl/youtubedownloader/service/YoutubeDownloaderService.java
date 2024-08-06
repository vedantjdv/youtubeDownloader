package com.ytdl.youtubedownloader.service;

import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface YoutubeDownloaderService {

    public ResponseEntity<Resource> downloadVideo(@RequestBody Map<String, String> request);
} 
