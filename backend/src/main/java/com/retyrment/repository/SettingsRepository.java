package com.retyrment.repository;

import com.retyrment.model.Settings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends MongoRepository<Settings, String> {
    
    java.util.Optional<Settings> findByUserId(String userId);
    
    // Settings will typically have only one document per user
}
