package com.retyrment.repository;

import com.retyrment.model.Loan;
import com.retyrment.model.Loan.LoanType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends MongoRepository<Loan, String> {
    
    List<Loan> findByUserId(String userId);
    
    List<Loan> findByUserIdAndRemainingMonthsGreaterThan(String userId, Integer months);
    
    List<Loan> findByType(LoanType type);
    
    List<Loan> findByRemainingMonthsGreaterThan(Integer months);
    
    List<Loan> findByOutstandingAmountGreaterThan(Double amount);
    
    Optional<Loan> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    void deleteByUserId(String userId);
}
