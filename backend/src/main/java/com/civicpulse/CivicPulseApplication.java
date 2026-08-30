package com.civicpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CivicPulse — Community Event & Engagement Platform.
 * 
 * Copyright © 2026 Jai Sai Vardhan Reddy. All rights reserved.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CivicPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicPulseApplication.class, args);
    }
}
