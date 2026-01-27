package com.retyrment.repository;

import com.retyrment.model.Investment;
import com.retyrment.model.Investment.InvestmentType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends MongoRepository<Investment, String> {
    
    List<Investment> findByUserId(String userId);
    
    List<Investment> findByUserIdAndType(String userId, InvestmentType type);
    
    List<Investment> findByUserIdAndMonthlySipGreaterThan(String userId, Double amount);
    
    List<Investment> findByType(InvestmentType type);
    
    List<Investment> findByNameContainingIgnoreCase(String name);
    
    List<Investment> findByMonthlySipGreaterThan(Double amount);
    
    Optional<Investment> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
    
    void deleteByUserId(String userId);
}
