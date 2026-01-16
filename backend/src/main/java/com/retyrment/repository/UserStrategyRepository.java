package com.retyrment.repository;

import com.retyrment.model.UserStrategy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStrategyRepository extends MongoRepository<UserStrategy, String> {
    Optional<UserStrategy> findByUserId(String userId);
}
