package com.retyrment.repository;

import com.retyrment.model.Goal;
import com.retyrment.model.Goal.Priority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    
    List<Goal> findByUserId(String userId);
    
    List<Goal> findByUserIdOrderByTargetYearAsc(String userId);
    
    List<Goal> findByUserIdAndIsRecurringTrue(String userId);
    
    List<Goal> findByPriority(Priority priority);
    
    List<Goal> findByTargetYearLessThanEqual(Integer year);
    
    List<Goal> findByIsRecurringTrue();
    
    List<Goal> findAllByOrderByTargetYearAsc();
    
    Optional<Goal> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    void deleteByUserId(String userId);
}
