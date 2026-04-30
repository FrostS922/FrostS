package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.Defect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DefectRepository extends JpaRepository<Defect, Long> {

    Page<Defect> findByProjectId(Long projectId, Pageable pageable);

    Page<Defect> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);

    Page<Defect> findByProjectIdAndSeverity(Long projectId, String severity, Pageable pageable);

    Page<Defect> findByProjectIdAndAssignedTo(Long projectId, String assignedTo, Pageable pageable);

    List<Defect> findByProjectIdAndStatus(Long projectId, String status);

    @Query("SELECT d FROM Defect d WHERE d.project.id = :projectId AND d.createdAt BETWEEN :start AND :end")
    List<Defect> findByProjectIdAndCreatedAtBetween(@Param("projectId") Long projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByProjectIdAndStatus(Long projectId, String status);

    long countByProjectIdAndSeverity(Long projectId, String severity);

    boolean existsByDefectNumber(String defectNumber);
}
