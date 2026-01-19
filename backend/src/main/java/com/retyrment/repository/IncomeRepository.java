package com.retyrment.repository;

import com.retyrment.model.Income;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends MongoRepository<Income, String> {
    
    List<Income> findByUserId(String userId);
    
    List<Income> findByUserIdAndIsActiveTrue(String userId);
    
    List<Income> findByIsActiveTrue();
    
    List<Income> findBySourceContainingIgnoreCase(String source);
    
    Optional<Income> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    void deleteByUserId(String userId);
}
