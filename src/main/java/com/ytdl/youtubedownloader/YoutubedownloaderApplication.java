package com.ytdl.youtubedownloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class YoutubedownloaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoutubedownloaderApplication.class, args);
	}

}
