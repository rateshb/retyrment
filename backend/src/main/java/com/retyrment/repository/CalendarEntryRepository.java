package com.retyrment.repository;

import com.retyrment.model.CalendarEntry;
import com.retyrment.model.CalendarEntry.CalendarCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarEntryRepository extends MongoRepository<CalendarEntry, String> {
    
    List<CalendarEntry> findByUserId(String userId);
    
    List<CalendarEntry> findByUserIdAndIsActiveTrue(String userId);
    
    List<CalendarEntry> findByCategory(CalendarCategory category);
    
    List<CalendarEntry> findByIsActiveTrue();
    
    List<CalendarEntry> findByDueMonthsContaining(Integer month);
    
    List<CalendarEntry> findByAutoLinkedFalse();
}
