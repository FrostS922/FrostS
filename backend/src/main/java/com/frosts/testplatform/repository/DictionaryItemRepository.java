package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    Optional<DictionaryItem> findByIdAndIsDeletedFalse(Long id);

    List<DictionaryItem> findByTypeIdAndIsDeletedFalseOrderBySortOrderAsc(Long typeId);

    List<DictionaryItem> findByTypeIdAndEnabledAndIsDeletedFalseOrderBySortOrderAsc(Long typeId, Boolean enabled);

    Page<DictionaryItem> findByTypeIdAndIsDeletedFalse(Long typeId, Pageable pageable);

    boolean existsByTypeIdAndCodeAndIsDeletedFalse(Long typeId, String code);

    @Query("""
            SELECT di FROM DictionaryItem di
            WHERE di.typeId = :typeId
              AND di.isDeleted = false
              AND (
                LOWER(di.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(di.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<DictionaryItem> searchByTypeIdAndKeyword(@Param("typeId") Long typeId, @Param("keyword") String keyword, Pageable pageable);

    long countByTypeIdAndIsDeletedFalse(Long typeId);
}
