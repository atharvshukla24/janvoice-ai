package com.janvoice.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class to enable Global CORS (Cross-Origin Resource Sharing).
 * This ensures our React application running on http://localhost:5173
 * can make API requests to our Spring Boot server on http://localhost:8080.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Allow CORS on all endpoints
                        .allowedOrigins("*") // In development, allow all origins (CORS wildcard is fine for MVP
                                             // hackathon)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization");
            }
        };
    }
}
