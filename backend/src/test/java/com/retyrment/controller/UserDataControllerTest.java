package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.UserDataDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDataController Tests")
class UserDataControllerTest {

    @Mock
    private UserDataDeletionService userDataDeletionService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserDataController userDataController;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_USER_EMAIL);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    @DisplayName("Should return data summary successfully")
    void testGetDataSummary_Success() {
        // Arrange
        Map<String, Object> expectedSummary = new HashMap<>();
        expectedSummary.put("incomes", 5);
        expectedSummary.put("investments", 10);
        expectedSummary.put("loans", 2);
        expectedSummary.put("totalRecords", 17);

        when(userDataDeletionService.getUserDataSummary(TEST_USER_ID)).thenReturn(expectedSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.getDataSummary();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().get("incomes"));
        assertEquals(10, response.getBody().get("investments"));
        assertEquals(2, response.getBody().get("loans"));
        assertEquals(17, response.getBody().get("totalRecords"));

        verify(userDataDeletionService).getUserDataSummary(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle exception when getting data summary")
    void testGetDataSummary_Exception() {
        // Arrange
        when(userDataDeletionService.getUserDataSummary(TEST_USER_ID))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.getDataSummary();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to get data summary", response.getBody().get("error"));
        assertEquals("Database connection failed", response.getBody().get("message"));

        verify(userDataDeletionService).getUserDataSummary(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should successfully delete all data with valid confirmation")
    void testDeleteAllData_Success() {
        // Arrange
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("success", true);
        expectedResult.put("totalDeleted", 25);
        expectedResult.put("message", "All user data deleted successfully");

        when(userDataDeletionService.deleteAllUserData(TEST_USER_ID)).thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("DELETE_ALL_DATA");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(25, response.getBody().get("totalDeleted"));

        verify(userDataDeletionService).deleteAllUserData(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should reject deletion with invalid confirmation")
    void testDeleteAllData_InvalidConfirmation() {
        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("INVALID_TOKEN");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid confirmation", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("DELETE_ALL_DATA"));

        // Verify service was never called
        verify(userDataDeletionService, never()).deleteAllUserData(anyString());
    }

    @Test
    @DisplayName("Should reject deletion with empty confirmation")
    void testDeleteAllData_EmptyConfirmation() {
        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid confirmation", response.getBody().get("error"));

        verify(userDataDeletionService, never()).deleteAllUserData(anyString());
    }

    @Test
    @DisplayName("Should reject deletion with null confirmation")
    void testDeleteAllData_NullConfirmation() {
        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData(null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid confirmation", response.getBody().get("error"));

        verify(userDataDeletionService, never()).deleteAllUserData(anyString());
    }

    @Test
    @DisplayName("Should handle service returning failure on deletion")
    void testDeleteAllData_ServiceFailure() {
        // Arrange
        Map<String, Object> failureResult = new HashMap<>();
        failureResult.put("success", false);
        failureResult.put("error", "Transaction rolled back");
        failureResult.put("message", "Failed to delete some records");

        when(userDataDeletionService.deleteAllUserData(TEST_USER_ID)).thenReturn(failureResult);

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("DELETE_ALL_DATA");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Transaction rolled back", response.getBody().get("error"));

        verify(userDataDeletionService).deleteAllUserData(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle exception during deletion")
    void testDeleteAllData_Exception() {
        // Arrange
        when(userDataDeletionService.deleteAllUserData(TEST_USER_ID))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("DELETE_ALL_DATA");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Failed to delete user data", response.getBody().get("error"));
        assertEquals("Unexpected database error", response.getBody().get("message"));

        verify(userDataDeletionService).deleteAllUserData(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle authentication with different user types")
    void testGetDataSummary_WithDifferentUsers() {
        // Arrange
        User adminUser = new User();
        adminUser.setId("admin-123");
        adminUser.setEmail("admin@example.com");

        when(authentication.getPrincipal()).thenReturn(adminUser);

        Map<String, Object> expectedSummary = new HashMap<>();
        expectedSummary.put("totalRecords", 0);

        when(userDataDeletionService.getUserDataSummary("admin-123")).thenReturn(expectedSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.getDataSummary();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userDataDeletionService).getUserDataSummary("admin-123");
    }

    @Test
    @DisplayName("Should preserve user email in logs during deletion")
    void testDeleteAllData_LogsUserEmail() {
        // Arrange
        Map<String, Object> successResult = new HashMap<>();
        successResult.put("success", true);
        successResult.put("totalDeleted", 10);

        when(userDataDeletionService.deleteAllUserData(TEST_USER_ID)).thenReturn(successResult);

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.deleteAllData("DELETE_ALL_DATA");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        
        // Verify the service was called with correct user ID
        verify(userDataDeletionService).deleteAllUserData(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle case-sensitive confirmation token")
    void testDeleteAllData_CaseSensitiveToken() {
        // Test lowercase
        ResponseEntity<Map<String, Object>> response1 = userDataController.deleteAllData("delete_all_data");
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());

        // Test mixed case
        ResponseEntity<Map<String, Object>> response2 = userDataController.deleteAllData("Delete_All_Data");
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());

        // Test with spaces
        ResponseEntity<Map<String, Object>> response3 = userDataController.deleteAllData("DELETE ALL DATA");
        assertEquals(HttpStatus.BAD_REQUEST, response3.getStatusCode());

        // Verify service was never called for invalid tokens
        verify(userDataDeletionService, never()).deleteAllUserData(anyString());
    }

    @Test
    @DisplayName("Should return detailed error information on exception")
    void testGetDataSummary_DetailedErrorInfo() {
        // Arrange
        String errorMessage = "MongoDB connection timeout after 30 seconds";
        when(userDataDeletionService.getUserDataSummary(TEST_USER_ID))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<Map<String, Object>> response = userDataController.getDataSummary();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Failed to get data summary", body.get("error"));
        assertEquals(errorMessage, body.get("message"));
        
        // Verify error structure
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
    }
}
