package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByNameContainingOrCodeContainingAndIsDeletedFalse(String name, String code, Pageable pageable);

    Page<Project> findByIsDeletedFalse(Pageable pageable);

    List<Project> findByIsDeletedFalse();

    boolean existsByCode(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.members WHERE p.id = :id AND p.isDeleted = false")
    Project findByIdWithMembers(Long id);
}
