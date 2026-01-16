package com.retyrment.repository;

import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
    
    List<Expense> findByUserId(String userId);
    
    List<Expense> findByUserIdAndIsFixedTrue(String userId);
    
    List<Expense> findByUserIdAndIsFixedFalse(String userId);
    
    List<Expense> findByCategory(ExpenseCategory category);
    
    List<Expense> findByIsFixedTrue();
    
    List<Expense> findByIsFixedFalse();
    
    Optional<Expense> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
}
