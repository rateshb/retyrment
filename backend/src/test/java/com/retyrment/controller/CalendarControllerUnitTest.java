package com.retyrment.controller;

import com.retyrment.model.CalendarEntry;
import com.retyrment.repository.CalendarEntryRepository;
import com.retyrment.service.CalendarService;
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
import com.retyrment.model.User;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarController Unit Tests")
class CalendarControllerUnitTest {

    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    @Mock
    private CalendarService calendarService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CalendarController calendarController;

    private CalendarEntry testEntry;
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
        testEntry = CalendarEntry.builder()
                .id("entry-1")
                .description("SIP Payment")
                .amount(10000.0)
                .category(CalendarEntry.CalendarCategory.SIP)
                .autoLinked(true)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getFullYearCalendar")
    class GetFullYearCalendar {
        @Test
        @DisplayName("should return full year calendar")
        void shouldReturnFullYearCalendar() {
            Map<String, Object> mockCalendar = new HashMap<>();
            mockCalendar.put("year", 2026);
            mockCalendar.put("months", new ArrayList<>());
            when(calendarService.generateYearCalendar("user-1")).thenReturn(mockCalendar);

            Map<String, Object> result = calendarController.getFullYearCalendar();

            assertThat(result).containsKey("year");
            verify(calendarService).generateYearCalendar("user-1");
        }
    }

    @Nested
    @DisplayName("getMonthCalendar")
    class GetMonthCalendar {
        @Test
        @DisplayName("should return month calendar")
        void shouldReturnMonthCalendar() {
            Map<String, Object> mockMonth = new HashMap<>();
            mockMonth.put("month", 1);
            mockMonth.put("entries", new ArrayList<>());
            when(calendarService.getMonthCalendar("user-1", 1)).thenReturn(mockMonth);

            Map<String, Object> result = calendarController.getMonthCalendar(1);

            assertThat(result).containsKey("month");
            verify(calendarService).getMonthCalendar("user-1", 1);
        }
    }

    @Nested
    @DisplayName("getUpcomingPayments")
    class GetUpcomingPayments {
        @Test
        @DisplayName("should return upcoming payments")
        void shouldReturnUpcomingPayments() {
            List<Map<String, Object>> mockPayments = new ArrayList<>();
            Map<String, Object> payment = new HashMap<>();
            payment.put("title", "SIP Payment");
            payment.put("amount", 10000.0);
            mockPayments.add(payment);
            when(calendarService.getUpcomingPayments("user-1", 30)).thenReturn(mockPayments);

            List<Map<String, Object>> result = calendarController.getUpcomingPayments();

            assertThat(result).hasSize(1);
            verify(calendarService).getUpcomingPayments("user-1", 30);
        }
    }

    @Nested
    @DisplayName("getManualEntries")
    class GetManualEntries {
        @Test
        @DisplayName("should return manual entries only")
        void shouldReturnManualEntries() {
            CalendarEntry manual = CalendarEntry.builder().id("manual-1").autoLinked(false).build();
            when(calendarEntryRepository.findByAutoLinkedFalse()).thenReturn(Arrays.asList(manual));

            List<CalendarEntry> result = calendarController.getManualEntries();

            assertThat(result).hasSize(1);
            verify(calendarEntryRepository).findByAutoLinkedFalse();
        }
    }

    @Nested
    @DisplayName("getEntryById")
    class GetEntryById {
        @Test
        @DisplayName("should return entry when found")
        void shouldReturnEntryWhenFound() {
            when(calendarEntryRepository.findById("entry-1")).thenReturn(Optional.of(testEntry));

            ResponseEntity<CalendarEntry> result = calendarController.getEntryById("entry-1");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getId()).isEqualTo("entry-1");
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturnNotFoundWhenMissing() {
            when(calendarEntryRepository.findById("invalid-id")).thenReturn(Optional.empty());

            ResponseEntity<CalendarEntry> result = calendarController.getEntryById("invalid-id");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createEntry")
    class CreateEntry {
        @Test
        @DisplayName("should create new entry with defaults")
        void shouldCreateEntryWithDefaults() {
            CalendarEntry newEntry = CalendarEntry.builder().description("New Payment").amount(5000.0).build();
            when(calendarEntryRepository.save(any(CalendarEntry.class))).thenAnswer(inv -> {
                CalendarEntry e = inv.getArgument(0);
                e.setId("new-id");
                return e;
            });

            CalendarEntry result = calendarController.createEntry(newEntry);

            assertThat(result.getAutoLinked()).isFalse();
            assertThat(result.getIsActive()).isTrue();
            verify(calendarEntryRepository).save(any(CalendarEntry.class));
        }

        @Test
        @DisplayName("should set default isActive to true when null")
        void shouldSetDefaultIsActiveWhenNull() {
            CalendarEntry newEntry = CalendarEntry.builder()
                    .description("New Payment")
                    .amount(5000.0)
                    .isActive(null)
                    .build();
            
            when(calendarEntryRepository.save(any(CalendarEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            calendarController.createEntry(newEntry);

            verify(calendarEntryRepository).save(argThat(entry -> entry.getIsActive().equals(true)));
        }

        @Test
        @DisplayName("should preserve isActive when provided as false")
        void shouldPreserveIsActiveWhenProvidedAsFalse() {
            CalendarEntry newEntry = CalendarEntry.builder()
                    .description("New Payment")
                    .amount(5000.0)
                    .isActive(false)
                    .build();
            
            when(calendarEntryRepository.save(any(CalendarEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            calendarController.createEntry(newEntry);

            verify(calendarEntryRepository).save(argThat(entry -> entry.getIsActive().equals(false)));
        }

        @Test
        @DisplayName("should preserve isActive when provided as true")
        void shouldPreserveIsActiveWhenProvidedAsTrue() {
            CalendarEntry newEntry = CalendarEntry.builder()
                    .description("New Payment")
                    .amount(5000.0)
                    .isActive(true)
                    .build();
            
            when(calendarEntryRepository.save(any(CalendarEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            calendarController.createEntry(newEntry);

            verify(calendarEntryRepository).save(argThat(entry -> entry.getIsActive().equals(true)));
        }
    }

    @Nested
    @DisplayName("updateEntry")
    class UpdateEntry {
        @Test
        @DisplayName("should update existing entry")
        void shouldUpdateExistingEntry() {
            when(calendarEntryRepository.existsById("entry-1")).thenReturn(true);
            when(calendarEntryRepository.save(any(CalendarEntry.class))).thenReturn(testEntry);

            ResponseEntity<CalendarEntry> result = calendarController.updateEntry("entry-1", testEntry);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(calendarEntryRepository).save(any(CalendarEntry.class));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent entry")
        void shouldReturnNotFoundWhenUpdatingMissing() {
            when(calendarEntryRepository.existsById("invalid-id")).thenReturn(false);

            ResponseEntity<CalendarEntry> result = calendarController.updateEntry("invalid-id", testEntry);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteEntry")
    class DeleteEntry {
        @Test
        @DisplayName("should delete existing entry")
        void shouldDeleteExistingEntry() {
            when(calendarEntryRepository.existsById("entry-1")).thenReturn(true);
            doNothing().when(calendarEntryRepository).deleteById("entry-1");

            ResponseEntity<Void> result = calendarController.deleteEntry("entry-1");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(calendarEntryRepository).deleteById("entry-1");
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent entry")
        void shouldReturnNotFoundWhenDeletingMissing() {
            when(calendarEntryRepository.existsById("invalid-id")).thenReturn(false);

            ResponseEntity<Void> result = calendarController.deleteEntry("invalid-id");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
