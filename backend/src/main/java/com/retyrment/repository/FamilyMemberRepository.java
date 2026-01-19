package com.retyrment.repository;

import com.retyrment.model.FamilyMember;
import com.retyrment.model.FamilyMember.Relationship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends MongoRepository<FamilyMember, String> {
    
    List<FamilyMember> findByUserId(String userId);
    
    List<FamilyMember> findByUserIdOrderByDateOfBirthAsc(String userId);
    
    Optional<FamilyMember> findByIdAndUserId(String id, String userId);
    
    Optional<FamilyMember> findByUserIdAndRelationship(String userId, Relationship relationship);
    
    List<FamilyMember> findByUserIdAndRelationshipIn(String userId, List<Relationship> relationships);
    
    List<FamilyMember> findByUserIdAndIsDependentTrue(String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    void deleteByIdAndUserId(String id, String userId);
    
    void deleteByUserId(String userId);
    
    // Count methods for quick checks
    long countByUserId(String userId);
    
    long countByUserIdAndRelationship(String userId, Relationship relationship);
}
