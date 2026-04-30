package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {

    Optional<OrganizationUnit> findByIdAndIsDeletedFalse(Long id);

    Optional<OrganizationUnit> findByCode(String code);

    List<OrganizationUnit> findByIsDeletedFalseOrderBySortOrderAscCreatedAtAsc();

    boolean existsByCode(String code);

    long countByIsDeletedFalse();

    long countByParent_IdAndIsDeletedFalse(Long parentId);
}
