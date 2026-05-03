package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DictionaryLogRepository extends JpaRepository<DictionaryLog, Long> {

    Page<DictionaryLog> findByTypeIdOrderByOperatedAtDesc(Long typeId, Pageable pageable);

    @Query("""
            SELECT dl FROM DictionaryLog dl
            WHERE dl.operatedAt BETWEEN :startTime AND :endTime
            ORDER BY dl.operatedAt DESC
            """)
    Page<DictionaryLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    Page<DictionaryLog> findByOperatorOrderByOperatedAtDesc(String operator, Pageable pageable);
}
