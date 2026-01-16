package com.retyrment.repository;

import com.retyrment.model.UserFeatureAccess;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserFeatureAccessRepository extends MongoRepository<UserFeatureAccess, String> {
    Optional<UserFeatureAccess> findByUserId(String userId);
    void deleteByUserId(String userId);
}
