package com.retyrment.controller;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Unit Tests")
class HealthControllerUnitTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MongoDatabase mongoDatabase;

    @InjectMocks
    private HealthController healthController;

    @Nested
    @DisplayName("health")
    class Health {
        @Test
        @DisplayName("should return UP status")
        void shouldReturnUpStatus() {
            ResponseEntity<Map<String, Object>> result = healthController.health();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("status")).isEqualTo("UP");
            assertThat(result.getBody().get("service")).isEqualTo("Retyrment API");
            assertThat(result.getBody()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("detailedHealth")
    class DetailedHealth {
        @Test
        @DisplayName("should return UP when MongoDB is connected")
        void shouldReturnUpWhenMongoConnected() {
            when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
            when(mongoDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));
            when(mongoDatabase.getName()).thenReturn("retyrment");

            ResponseEntity<Map<String, Object>> result = healthController.detailedHealth();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("status")).isEqualTo("UP");
            assertThat(result.getBody()).containsKey("mongodb");
            Map<String, Object> mongodb = (Map<String, Object>) result.getBody().get("mongodb");
            assertThat(mongodb.get("status")).isEqualTo("UP");
        }

        @Test
        @DisplayName("should return DOWN when MongoDB is disconnected")
        void shouldReturnDownWhenMongoDisconnected() {
            when(mongoTemplate.getDb()).thenThrow(new RuntimeException("Connection refused"));

            ResponseEntity<Map<String, Object>> result = healthController.detailedHealth();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("status")).isEqualTo("DOWN");
            Map<String, Object> mongodb = (Map<String, Object>) result.getBody().get("mongodb");
            assertThat(mongodb.get("status")).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("should include memory information")
        void shouldIncludeMemoryInfo() {
            when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
            when(mongoDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));
            when(mongoDatabase.getName()).thenReturn("retyrment");

            ResponseEntity<Map<String, Object>> result = healthController.detailedHealth();

            assertThat(result.getBody()).containsKey("memory");
            Map<String, Object> memory = (Map<String, Object>) result.getBody().get("memory");
            assertThat(memory).containsKeys("total", "free", "used", "max");
        }
    }
}
