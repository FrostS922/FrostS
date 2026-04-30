package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.TestPlanCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPlanCaseRepository extends JpaRepository<TestPlanCase, Long> {

    List<TestPlanCase> findByTestPlanId(Long testPlanId);

    List<TestPlanCase> findByTestPlanIdAndStatus(Long testPlanId, String status);

    List<TestPlanCase> findByTestPlanIdAndAssignedTo(Long testPlanId, String assignedTo);

    @Query("SELECT tpc FROM TestPlanCase tpc WHERE tpc.testPlan.id = :testPlanId AND tpc.testCase.id = :testCaseId")
    TestPlanCase findByTestPlanIdAndTestCaseId(@Param("testPlanId") Long testPlanId, @Param("testCaseId") Long testCaseId);

    long countByTestPlanIdAndStatus(Long testPlanId, String status);
}
