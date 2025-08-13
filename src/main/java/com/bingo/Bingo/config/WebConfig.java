package com.bingo.Bingo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
           @Override
            public void addCorsMappings(CorsRegistry corsRegistry){
               corsRegistry.addMapping("/**")
                          .allowedOrigins("*")
                          .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                          .allowedHeaders("*")
                          .allowCredentials(false);
           }
        };
    }
}
