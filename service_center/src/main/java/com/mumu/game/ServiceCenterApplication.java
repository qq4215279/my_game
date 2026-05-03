package com.mumu.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ServiceCenterApplication
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/2 18:56
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ServiceCenterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceCenterApplication.class, args);
    }
}
