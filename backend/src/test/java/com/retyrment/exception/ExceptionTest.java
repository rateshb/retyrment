package com.retyrment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionTest {

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTest {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
            assertThat(ex.getMessage()).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("should create with resource name and id")
        void shouldCreateWithResourceNameAndId() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Investment", "123");
            assertThat(ex.getMessage()).isEqualTo("Investment not found with id: 123");
        }

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            assertThatThrownBy(() -> {
                throw new ResourceNotFoundException("Test");
            }).isInstanceOf(ResourceNotFoundException.class)
              .hasMessage("Test");
        }
    }

    @Nested
    @DisplayName("BusinessException")
    class BusinessExceptionTest {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            BusinessException ex = new BusinessException("Business rule violated");
            assertThat(ex.getMessage()).isEqualTo("Business rule violated");
        }

        @Test
        @DisplayName("should create with message and cause")
        void shouldCreateWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Original error");
            BusinessException ex = new BusinessException("Business error", cause);
            assertThat(ex.getMessage()).isEqualTo("Business error");
            assertThat(ex.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            assertThatThrownBy(() -> {
                throw new BusinessException("Validation failed");
            }).isInstanceOf(BusinessException.class)
              .hasMessage("Validation failed");
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTest {

        @Test
        @DisplayName("should create with builder")
        void shouldCreateWithBuilder() {
            LocalDateTime now = LocalDateTime.now();
            ErrorResponse response = ErrorResponse.builder()
                    .timestamp(now)
                    .status(400)
                    .error("Bad Request")
                    .message("Validation failed")
                    .path("/api/test")
                    .build();
            
            assertThat(response.getTimestamp()).isEqualTo(now);
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getError()).isEqualTo("Bad Request");
            assertThat(response.getMessage()).isEqualTo("Validation failed");
            assertThat(response.getPath()).isEqualTo("/api/test");
        }

        @Test
        @DisplayName("should create with field errors")
        void shouldCreateWithFieldErrors() {
            Map<String, String> fieldErrors = new HashMap<>();
            fieldErrors.put("name", "Name is required");
            fieldErrors.put("amount", "Amount must be positive");
            
            ErrorResponse response = ErrorResponse.builder()
                    .status(400)
                    .message("Validation failed")
                    .fieldErrors(fieldErrors)
                    .build();
            
            assertThat(response.getFieldErrors()).hasSize(2);
            assertThat(response.getFieldErrors()).containsEntry("name", "Name is required");
        }

        @Test
        @DisplayName("should allow setting field errors")
        void shouldAllowSettingFieldErrors() {
            ErrorResponse response = new ErrorResponse();
            
            Map<String, String> fieldErrors = new HashMap<>();
            fieldErrors.put("field", "Error message");
            response.setFieldErrors(fieldErrors);
            
            assertThat(response.getFieldErrors()).containsEntry("field", "Error message");
        }

        @Test
        @DisplayName("should use no-args constructor")
        void shouldUseNoArgsConstructor() {
            ErrorResponse response = new ErrorResponse();
            response.setStatus(404);
            response.setMessage("Not Found");
            
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getMessage()).isEqualTo("Not Found");
        }

        @Test
        @DisplayName("should use all-args constructor")
        void shouldUseAllArgsConstructor() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, String> fieldErrors = new HashMap<>();
            
            ErrorResponse response = new ErrorResponse(
                    404, "Not Found", "Resource not found", now, "/api/resource", fieldErrors);
            
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getError()).isEqualTo("Not Found");
            assertThat(response.getMessage()).isEqualTo("Resource not found");
        }
    }
}
