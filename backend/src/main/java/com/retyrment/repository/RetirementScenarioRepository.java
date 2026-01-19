package com.retyrment.repository;

import com.retyrment.model.RetirementScenario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetirementScenarioRepository extends MongoRepository<RetirementScenario, String> {
    
    java.util.List<RetirementScenario> findByUserId(String userId);
    
    Optional<RetirementScenario> findByUserIdAndIsDefaultTrue(String userId);
    
    Optional<RetirementScenario> findByIsDefaultTrue();
    
    Optional<RetirementScenario> findByNameIgnoreCase(String name);
    
    void deleteByUserId(String userId);
}
