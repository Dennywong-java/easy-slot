package com.easyslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Easy Slot application entry point
 */
@SpringBootApplication
@EnableScheduling
public class EasySlotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasySlotApplication.class, args);
    }
} 