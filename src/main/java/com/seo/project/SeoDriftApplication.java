package com.seo.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class SeoDriftApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeoDriftApplication.class, args);
	}

	@Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

}
