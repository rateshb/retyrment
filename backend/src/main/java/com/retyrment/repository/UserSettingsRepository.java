package com.retyrment.repository;

import com.retyrment.model.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {
    Optional<UserSettings> findByUserId(String userId);
    void deleteByUserId(String userId);
}
