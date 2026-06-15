package com.seo.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import jakarta.servlet.MultipartConfigElement;

@SpringBootApplication
public class SeoDriftApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeoDriftApplication.class, args);
	}

	@Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

	@Bean(name = "multipartResolver")
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		String uploadPath = System.getProperty("user.home") + "/.seodrift/uploads";
		java.io.File uploadDir = new java.io.File(uploadPath);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}
		return new MultipartConfigElement(
			uploadPath,
			2097152000L, // 2000MB
			2097152000L, // 2000MB
			0
		);
	}

}
