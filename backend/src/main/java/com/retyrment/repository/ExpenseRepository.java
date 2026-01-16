package com.retyrment.repository;

import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
    
    List<Expense> findByUserId(String userId);
    
    List<Expense> findByUserIdAndIsFixedTrue(String userId);
    
    List<Expense> findByUserIdAndIsFixedFalse(String userId);
    
    List<Expense> findByCategory(ExpenseCategory category);
    
    List<Expense> findByUserIdAndCategory(String userId, ExpenseCategory category);
    
    List<Expense> findByIsFixedTrue();
    
    List<Expense> findByIsFixedFalse();
    
    Optional<Expense> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    // Time-bound expense queries
    List<Expense> findByUserIdAndIsTimeBoundTrue(String userId);
    
    List<Expense> findByUserIdAndIsTimeBoundFalse(String userId);
    
    // Find expenses by dependent
    List<Expense> findByUserIdAndDependentName(String userId, String dependentName);
    
    // Find expenses that end in a specific year
    List<Expense> findByUserIdAndEndYear(String userId, Integer endYear);
    
    // Find expenses that end before a specific year (will free up money)
    @Query("{ 'userId': ?0, 'isTimeBound': true, 'endYear': { $lte: ?1 } }")
    List<Expense> findExpensesEndingByYear(String userId, Integer year);
    
    // Find expenses that continue after a specific year
    @Query("{ 'userId': ?0, $or: [ { 'isTimeBound': false }, { 'isTimeBound': null }, { 'endYear': { $gt: ?1 } }, { 'endYear': null } ] }")
    List<Expense> findExpensesContinuingAfterYear(String userId, Integer year);
    
    // Find education-related expenses
    @Query("{ 'userId': ?0, 'category': { $in: ['SCHOOL_FEE', 'COLLEGE_FEE', 'TUITION', 'COACHING', 'BOOKS_SUPPLIES', 'HOSTEL'] } }")
    List<Expense> findEducationExpenses(String userId);
    
    // Find dependent care expenses
    @Query("{ 'userId': ?0, 'category': { $in: ['CHILDCARE', 'DAYCARE', 'ELDERLY_CARE'] } }")
    List<Expense> findDependentCareExpenses(String userId);
}
