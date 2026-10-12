package com.textile.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class TextileAccountsApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TextileAccountsApplication.class);

        String activeProfiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        boolean isResetPassword = (activeProfiles != null && activeProfiles.contains("reset-owner-password"))
                || Arrays.stream(args).anyMatch(arg -> arg.contains("reset-owner-password"));

        if (isResetPassword) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }

        app.run(args);
    }
}
