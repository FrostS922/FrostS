package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    Optional<Role> findByIdAndIsDeletedFalse(Long id);

    Page<Role> findByIsDeletedFalse(Pageable pageable);

    List<Role> findByIsDeletedFalse();

    @Query("""
            SELECT r FROM Role r
            WHERE r.isDeleted = false
              AND (
                LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Role> searchActiveRoles(@Param("search") String search, Pageable pageable);

    boolean existsByCode(String code);

    long countByIsDeletedFalse();
}
