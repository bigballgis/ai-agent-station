package com.aiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("com.aiagent.config.properties")
public class AiAgentPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentPlatformApplication.class, args);
    }
}
