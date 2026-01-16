package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping("/json")
    public Map<String, Object> exportAllDataAsJson() {
        String userId = getCurrentUserId();
        return exportService.exportAllData(userId);
    }

    @PostMapping("/import/json")
    public ResponseEntity<Map<String, String>> importDataFromJson(@RequestBody Map<String, Object> data) {
        try {
            String userId = getCurrentUserId();
            exportService.importAllData(userId, data);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Data imported successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdfReport() {
        try {
            String userId = getCurrentUserId();
            byte[] pdfBytes = exportService.generatePdfReport(userId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Retyrment_Report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcelReport() {
        try {
            String userId = getCurrentUserId();
            byte[] excelBytes = exportService.generateExcelReport(userId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Retyrment_Report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
