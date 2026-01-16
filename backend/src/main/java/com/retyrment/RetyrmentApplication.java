package com.retyrment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Retyrment - Personal Finance Planning Application.
 * This is a Spring Boot application that provides financial planning and analysis
 * features including retirement planning, investment tracking, and Monte Carlo simulations.
 *
 * @author Retyrment Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class RetyrmentApplication {

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(RetyrmentApplication.class, args);
    }
}
