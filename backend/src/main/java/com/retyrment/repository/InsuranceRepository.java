package com.retyrment.repository;

import com.retyrment.model.Insurance;
import com.retyrment.model.Insurance.InsuranceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceRepository extends MongoRepository<Insurance, String> {
    
    List<Insurance> findByUserId(String userId);
    
    List<Insurance> findByUserIdAndType(String userId, InsuranceType type);
    
    List<Insurance> findByType(InsuranceType type);
    
    List<Insurance> findByRenewalMonth(Integer month);
    
    List<Insurance> findByTypeIn(List<InsuranceType> types);
    
    List<Insurance> findByUserIdAndTypeIn(String userId, List<InsuranceType> types);
    
    Optional<Insurance> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
    
    void deleteByUserId(String userId);
}
