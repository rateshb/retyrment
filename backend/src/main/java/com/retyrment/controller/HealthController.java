package com.retyrment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring and load balancers.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final MongoTemplate mongoTemplate;

    /**
     * Basic health check - always returns OK if service is running.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "Retyrment API");
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check including database connectivity.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "Retyrment API");

        // Check MongoDB connectivity
        Map<String, Object> dbHealth = new LinkedHashMap<>();
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            dbHealth.put("status", "UP");
            dbHealth.put("database", mongoTemplate.getDb().getName());
        } catch (RuntimeException e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            health.put("status", "DOWN");
        }
        health.put("mongodb", dbHealth);

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("total", formatBytes(runtime.totalMemory()));
        memory.put("free", formatBytes(runtime.freeMemory()));
        memory.put("used", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        memory.put("max", formatBytes(runtime.maxMemory()));
        health.put("memory", memory);

        return ResponseEntity.ok(health);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
