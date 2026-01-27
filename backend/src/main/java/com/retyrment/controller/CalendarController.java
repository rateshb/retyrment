package com.retyrment.controller;

import com.retyrment.model.CalendarEntry;
import com.retyrment.repository.CalendarEntryRepository;
import com.retyrment.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController extends BaseController {

    private final CalendarEntryRepository calendarEntryRepository;
    private final CalendarService calendarService;

    @GetMapping
    public Map<String, Object> getFullYearCalendar() {
        String userId = getCurrentUserId();
        return calendarService.generateYearCalendar(userId);
    }

    @GetMapping("/month/{month}")
    public Map<String, Object> getMonthCalendar(@PathVariable Integer month) {
        String userId = getCurrentUserId();
        return calendarService.getMonthCalendar(userId, month);
    }

    @GetMapping("/upcoming")
    public List<Map<String, Object>> getUpcomingPayments() {
        String userId = getCurrentUserId();
        return calendarService.getUpcomingPayments(userId, 30);
    }

    @GetMapping("/entries")
    public List<CalendarEntry> getManualEntries() {
        return calendarEntryRepository.findByAutoLinkedFalse();
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<CalendarEntry> getEntryById(@PathVariable String id) {
        return calendarEntryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CalendarEntry createEntry(@RequestBody CalendarEntry entry) {
        entry.setAutoLinked(false);
        entry.setIsActive(entry.getIsActive() != null ? entry.getIsActive() : true);
        return calendarEntryRepository.save(entry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CalendarEntry> updateEntry(@PathVariable String id, 
                                                      @RequestBody CalendarEntry entry) {
        if (!calendarEntryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entry.setId(id);
        return ResponseEntity.ok(calendarEntryRepository.save(entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable String id) {
        if (!calendarEntryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        calendarEntryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
