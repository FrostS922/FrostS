package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.PerformanceLog;
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
public interface PerformanceLogRepository extends JpaRepository<PerformanceLog, Long> {

    Page<PerformanceLog> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<PerformanceLog> findByMetricNameAndIsDeletedFalseOrderByCreatedAtDesc(String metricName, Pageable pageable);

    @Query(value = """
            SELECT * FROM sys_performance_log p
            WHERE p.is_deleted = false
              AND (CAST(:metricName AS TEXT) IS NULL OR p.metric_name = CAST(:metricName AS TEXT))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR p.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR p.created_at <= CAST(:endTime AS TIMESTAMP))
            """,
            countQuery = """
            SELECT COUNT(*) FROM sys_performance_log p
            WHERE p.is_deleted = false
              AND (CAST(:metricName AS TEXT) IS NULL OR p.metric_name = CAST(:metricName AS TEXT))
              AND (CAST(:startTime AS TIMESTAMP) IS NULL OR p.created_at >= CAST(:startTime AS TIMESTAMP))
              AND (CAST(:endTime AS TIMESTAMP) IS NULL OR p.created_at <= CAST(:endTime AS TIMESTAMP))
            """,
            nativeQuery = true)
    Page<PerformanceLog> searchPerformanceLogs(@Param("metricName") String metricName,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               Pageable pageable);

    long countByIsDeletedFalse();

    long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime since);

    @Query("SELECT p.metricName, AVG(p.metricValue), MIN(p.metricValue), " +
            "MAX(p.metricValue), COUNT(p), " +
            "SUM(CASE WHEN p.rating = 'poor' THEN 1 ELSE 0 END) " +
            "FROM PerformanceLog p WHERE p.isDeleted = false " +
            "AND p.createdAt >= :since " +
            "GROUP BY p.metricName")
    List<Object[]> getMetricStats(@Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('DATE', p.createdAt) as date, p.metricName, AVG(p.metricValue) " +
            "FROM PerformanceLog p WHERE p.isDeleted = false " +
            "AND p.createdAt >= :since " +
            "GROUP BY FUNCTION('DATE', p.createdAt), p.metricName " +
            "ORDER BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> getMetricTrend(@Param("since") LocalDateTime since);

    long countByMetricNameAndRatingAndCreatedAtAfterAndIsDeletedFalse(String metricName, String rating, LocalDateTime since);

    long countByMetricNameAndCreatedAtAfterAndIsDeletedFalse(String metricName, LocalDateTime since);

    @Modifying
    @Query("UPDATE PerformanceLog p SET p.isDeleted = true WHERE p.createdAt < :cutoff AND p.isDeleted = false")
    int softDeleteBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p.metricValue FROM PerformanceLog p WHERE p.isDeleted = false " +
            "AND p.metricName = :metricName AND p.createdAt >= :since " +
            "ORDER BY p.metricValue ASC")
    List<Double> findValuesByMetricNameAndCreatedAtAfterAndIsDeletedFalse(
            @Param("metricName") String metricName, @Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT p.metricName FROM PerformanceLog p WHERE p.isDeleted = false AND p.createdAt >= :since")
    List<String> findDistinctMetricNamesSince(@Param("since") LocalDateTime since);

    @Query("SELECT p.metricName, AVG(p.metricValue), COUNT(p) " +
            "FROM PerformanceLog p WHERE p.isDeleted = false " +
            "AND p.createdAt >= :startTime AND p.createdAt < :endTime " +
            "GROUP BY p.metricName")
    List<Object[]> getMetricAvgBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
