package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.TestCaseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseModuleRepository extends JpaRepository<TestCaseModule, Long> {

    List<TestCaseModule> findByProjectId(Long projectId);

    List<TestCaseModule> findByProjectIdAndParentId(Long projectId, Long parentId);

    boolean existsByNameAndProjectId(String name, Long projectId);
}
