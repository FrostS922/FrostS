package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    Page<TestCase> findByProjectId(Long projectId, Pageable pageable);

    Page<TestCase> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);

    Page<TestCase> findByProjectIdAndModuleId(Long projectId, Long moduleId, Pageable pageable);

    List<TestCase> findByProjectIdAndStatus(Long projectId, String status);

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.id = :projectId AND tc.requirement.id = :requirementId")
    List<TestCase> findByProjectIdAndRequirementId(@Param("projectId") Long projectId, @Param("requirementId") Long requirementId);

    long countByProjectIdAndStatus(Long projectId, String status);

    long countByProjectId(Long projectId);

    List<TestCase> findByProjectId(Long projectId);

    boolean existsByCaseNumber(String caseNumber);

    long countByRequirementId(Long requirementId);
}
