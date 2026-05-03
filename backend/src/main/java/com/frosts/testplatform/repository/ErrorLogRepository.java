package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    Page<ErrorLog> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
            SELECT * FROM sys_error_log e
            WHERE e.is_deleted = false
              AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(CAST(e.error_message AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR e.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR e.created_at <= CAST(:endTime AS TIMESTAMP))
            """,
            countQuery = """
            SELECT COUNT(*) FROM sys_error_log e
            WHERE e.is_deleted = false
              AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(CAST(e.error_message AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR e.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR e.created_at <= CAST(:endTime AS TIMESTAMP))
            """,
            nativeQuery = true)
    Page<ErrorLog> searchErrors(@Param("keyword") String keyword,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                Pageable pageable);

    long countByErrorMessageAndCreatedAtAfterAndIsDeletedFalse(String errorMessage, LocalDateTime since);

    List<ErrorLog> findByIdInAndIsDeletedFalse(List<Long> ids);

    Page<ErrorLog> findByErrorMessageAndIsDeletedFalseOrderByCreatedAtDesc(String errorMessage, Pageable pageable);

    @Query("""
            SELECT DATE(e.createdAt) AS date, COUNT(e) AS count
            FROM ErrorLog e
            WHERE e.isDeleted = false
              AND e.createdAt >= :since
            GROUP BY DATE(e.createdAt)
            ORDER BY DATE(e.createdAt) ASC
            """)
    List<Object[]> countByDate(@Param("since") LocalDateTime since);

    long countByIsDeletedFalse();

    long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime since);

    List<ErrorLog> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE ErrorLog e SET e.isDeleted = true WHERE e.createdAt < :cutoff AND e.isDeleted = false")
    int softDeleteBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = """
            SELECT CAST(e.error_message AS TEXT) AS message, COUNT(e.id) AS cnt, MAX(e.created_at) AS last_seen, MIN(e.created_at) AS first_seen
            FROM sys_error_log e
            WHERE e.is_deleted = false
              AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(CAST(e.error_message AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR e.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR e.created_at <= CAST(:endTime AS TIMESTAMP))
            GROUP BY CAST(e.error_message AS TEXT)
            ORDER BY cnt DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT CAST(e.error_message AS TEXT)) FROM sys_error_log e
            WHERE e.is_deleted = false
              AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(CAST(e.error_message AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR e.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR e.created_at <= CAST(:endTime AS TIMESTAMP))
            """,
            nativeQuery = true)
    Page<Object[]> aggregateErrors(@Param("keyword") String keyword,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   Pageable pageable);

    @Query("SELECT e FROM ErrorLog e WHERE e.isDeleted = false " +
            "AND e.createdAt >= :startTime AND e.createdAt < :endTime " +
            "ORDER BY e.createdAt DESC")
    List<ErrorLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT e.category, COUNT(e) FROM ErrorLog e WHERE e.isDeleted = false " +
            "AND e.createdAt >= :startTime AND e.createdAt < :endTime " +
            "GROUP BY e.category ORDER BY COUNT(e) DESC")
    List<Object[]> countByCategoryInRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
