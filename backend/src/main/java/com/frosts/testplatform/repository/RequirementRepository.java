package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.Requirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    Page<Requirement> findByProjectId(Long projectId, Pageable pageable);

    Page<Requirement> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);

    List<Requirement> findByProjectIdAndParentIdIsNull(Long projectId);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId AND r.status = :status")
    List<Requirement> findByProjectIdAndStatusWithoutPaging(@Param("projectId") Long projectId, @Param("status") String status);

    long countByProjectIdAndStatus(Long projectId, String status);

    long countByProjectId(Long projectId);

    List<Requirement> findByProjectId(Long projectId);
}
