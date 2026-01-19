package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.FamilyMember;
import com.retyrment.model.User;
import com.retyrment.repository.FamilyMemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing family members.
 * Family member data is used for:
 * - Insurance recommendations (health, term life coverage)
 * - Dependent expense tracking
 * - Financial planning calculations
 */
@RestController
@RequestMapping("/family")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberRepository familyMemberRepository;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping
    public List<FamilyMember> getAllFamilyMembers() {
        String userId = getCurrentUserId();
        return familyMemberRepository.findByUserIdOrderByDateOfBirthAsc(userId);
    }

    @GetMapping("/{id}")
    public FamilyMember getFamilyMember(@PathVariable String id) {
        String userId = getCurrentUserId();
        return familyMemberRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));
    }

    @GetMapping("/dependents")
    public List<FamilyMember> getDependents() {
        String userId = getCurrentUserId();
        return familyMemberRepository.findByUserIdAndIsDependentTrue(userId);
    }

    @GetMapping("/self")
    public FamilyMember getSelf() {
        String userId = getCurrentUserId();
        return familyMemberRepository.findByUserIdAndRelationship(userId, FamilyMember.Relationship.SELF)
                .orElse(null);
    }

    @GetMapping("/spouse")
    public FamilyMember getSpouse() {
        String userId = getCurrentUserId();
        return familyMemberRepository.findByUserIdAndRelationship(userId, FamilyMember.Relationship.SPOUSE)
                .orElse(null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyMember createFamilyMember(@Valid @RequestBody FamilyMember member) {
        String userId = getCurrentUserId();
        member.setUserId(userId);
        
        // Set defaults
        if (member.getIsDependent() == null) {
            // Children and parents are typically dependents
            member.setIsDependent(
                member.getRelationship() == FamilyMember.Relationship.CHILD ||
                member.getRelationship() == FamilyMember.Relationship.PARENT ||
                member.getRelationship() == FamilyMember.Relationship.PARENT_IN_LAW
            );
        }
        
        if (member.getIsEarning() == null) {
            member.setIsEarning(false);
        }
        
        return familyMemberRepository.save(member);
    }

    @PutMapping("/{id}")
    public FamilyMember updateFamilyMember(@PathVariable String id, @Valid @RequestBody FamilyMember member) {
        String userId = getCurrentUserId();
        FamilyMember existing = familyMemberRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));
        
        member.setId(id);
        member.setUserId(userId);
        return familyMemberRepository.save(member);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFamilyMember(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!familyMemberRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("FamilyMember", id);
        }
        familyMemberRepository.deleteById(id);
    }
    
    @GetMapping("/summary")
    public FamilySummary getFamilySummary() {
        String userId = getCurrentUserId();
        List<FamilyMember> members = familyMemberRepository.findByUserId(userId);
        
        FamilySummary summary = new FamilySummary();
        summary.totalMembers = members.size();
        summary.dependents = (int) members.stream().filter(m -> Boolean.TRUE.equals(m.getIsDependent())).count();
        summary.earningMembers = (int) members.stream().filter(m -> Boolean.TRUE.equals(m.getIsEarning())).count();
        summary.children = (int) members.stream().filter(m -> m.getRelationship() == FamilyMember.Relationship.CHILD).count();
        summary.seniors = (int) members.stream().filter(FamilyMember::isSeniorCitizen).count();
        
        // Calculate total existing coverage
        summary.totalHealthCover = members.stream()
                .mapToDouble(m -> m.getExistingHealthCover() != null ? m.getExistingHealthCover() : 0)
                .sum();
        summary.totalLifeCover = members.stream()
                .mapToDouble(m -> m.getExistingLifeCover() != null ? m.getExistingLifeCover() : 0)
                .sum();
        
        return summary;
    }
    
    @lombok.Data
    public static class FamilySummary {
        int totalMembers;
        int dependents;
        int earningMembers;
        int children;
        int seniors;
        double totalHealthCover;
        double totalLifeCover;
    }
}
