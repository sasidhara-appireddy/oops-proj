package com.BitsBids;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArtGalleryBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtGalleryBackendApplication.class, args);
	}
	
	

}
