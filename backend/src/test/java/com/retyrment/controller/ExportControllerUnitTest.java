package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportController Unit Tests")
class ExportControllerUnitTest {

    @Mock
    private ExportService exportService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExportController exportController;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("exportAllDataAsJson")
    class ExportAllDataAsJson {
        @Test
        @DisplayName("should return all data as JSON")
        void shouldReturnAllDataAsJson() {
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("investments", new java.util.ArrayList<>());
            mockData.put("incomes", new java.util.ArrayList<>());
            when(exportService.exportAllData("user-1")).thenReturn(mockData);

            Map<String, Object> result = exportController.exportAllDataAsJson();

            assertThat(result).containsKeys("investments", "incomes");
            verify(exportService).exportAllData("user-1");
        }
    }

    @Nested
    @DisplayName("importDataFromJson")
    class ImportDataFromJson {
        @Test
        @DisplayName("should import data successfully")
        void shouldImportDataSuccessfully() {
            Map<String, Object> importData = new HashMap<>();
            importData.put("investments", new java.util.ArrayList<>());
            doNothing().when(exportService).importAllData(eq("user-1"), any());

            ResponseEntity<Map<String, String>> result = exportController.importDataFromJson(importData);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("status")).isEqualTo("success");
        }

        @Test
        @DisplayName("should return error on import failure")
        void shouldReturnErrorOnImportFailure() {
            Map<String, Object> importData = new HashMap<>();
            doThrow(new RuntimeException("Import failed")).when(exportService).importAllData(eq("user-1"), any());

            ResponseEntity<Map<String, String>> result = exportController.importDataFromJson(importData);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().get("status")).isEqualTo("error");
        }
    }

    @Nested
    @DisplayName("exportPdfReport")
    class ExportPdfReport {
        @Test
        @DisplayName("should return PDF bytes for summary")
        void shouldReturnPdfBytes() throws Exception {
            byte[] mockPdf = "PDF Content".getBytes();
            when(exportService.generateFinancialSummaryPdfReport("user-1")).thenReturn(mockPdf);

            ResponseEntity<byte[]> result = exportController.exportPdfReport("summary");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(mockPdf);
        }

        @Test
        @DisplayName("should return 500 on PDF generation failure")
        void shouldReturnServerErrorOnFailure() throws Exception {
            when(exportService.generateFinancialSummaryPdfReport("user-1")).thenThrow(new RuntimeException("PDF generation failed"));

            ResponseEntity<byte[]> result = exportController.exportPdfReport("summary");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("exportExcelReport")
    class ExportExcelReport {
        @Test
        @DisplayName("should return Excel bytes")
        void shouldReturnExcelBytes() throws Exception {
            byte[] mockExcel = "Excel Content".getBytes();
            when(exportService.generateExcelReport("user-1")).thenReturn(mockExcel);

            ResponseEntity<byte[]> result = exportController.exportExcelReport();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(mockExcel);
        }

        @Test
        @DisplayName("should return 500 on Excel generation failure")
        void shouldReturnServerErrorOnExcelFailure() throws Exception {
            when(exportService.generateExcelReport("user-1")).thenThrow(new RuntimeException("Excel generation failed"));

            ResponseEntity<byte[]> result = exportController.exportExcelReport();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
