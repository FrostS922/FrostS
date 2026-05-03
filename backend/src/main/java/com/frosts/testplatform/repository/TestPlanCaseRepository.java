package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.TestPlanCase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPlanCaseRepository extends JpaRepository<TestPlanCase, Long> {

    @EntityGraph(attributePaths = {"testCase", "testCase.module"})
    List<TestPlanCase> findByTestPlanId(Long testPlanId);

    @EntityGraph(attributePaths = {"testCase"})
    List<TestPlanCase> findByTestPlanIdAndStatus(Long testPlanId, String status);

    @EntityGraph(attributePaths = {"testCase"})
    List<TestPlanCase> findByTestPlanIdAndAssignedTo(Long testPlanId, String assignedTo);

    @Query("SELECT tpc FROM TestPlanCase tpc WHERE tpc.testPlan.id = :testPlanId AND tpc.testCase.id = :testCaseId")
    TestPlanCase findByTestPlanIdAndTestCaseId(@Param("testPlanId") Long testPlanId, @Param("testCaseId") Long testCaseId);

    long countByTestPlanIdAndStatus(Long testPlanId, String status);

    @EntityGraph(attributePaths = {"testCase", "testPlan"})
    List<TestPlanCase> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"testCase"})
    List<TestPlanCase> findByTestPlanIdAndTestCaseIdIn(Long testPlanId, List<Long> testCaseIds);

    @Query("SELECT tpc.testCase.id FROM TestPlanCase tpc WHERE tpc.testPlan.id = :testPlanId AND tpc.isDeleted = false")
    List<Long> findTestCaseIdsByTestPlanId(@Param("testPlanId") Long testPlanId);
}
