package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryTypeRepository extends JpaRepository<DictionaryType, Long> {

    Optional<DictionaryType> findByCodeAndIsDeletedFalse(String code);

    Optional<DictionaryType> findByIdAndIsDeletedFalse(Long id);

    List<DictionaryType> findByIsDeletedFalseOrderBySortOrderAsc();

    List<DictionaryType> findByParentIdAndIsDeletedFalseOrderBySortOrderAsc(Long parentId);

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("""
            SELECT dt FROM DictionaryType dt
            WHERE dt.isDeleted = false
              AND (
                LOWER(dt.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(dt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<DictionaryType> searchByKeyword(@Param("keyword") String keyword);
}
