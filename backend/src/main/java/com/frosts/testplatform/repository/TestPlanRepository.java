package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.TestPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, Long> {

    Page<TestPlan> findByProjectId(Long projectId, Pageable pageable);

    Page<TestPlan> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);

    List<TestPlan> findByProjectIdAndStatus(Long projectId, String status);
}
