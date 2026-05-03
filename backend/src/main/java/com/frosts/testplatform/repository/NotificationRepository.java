package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByIsGlobalTrueAndIsDeletedFalse(Pageable pageable);

    Page<Notification> findByTypeAndIsDeletedFalse(String type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.isGlobal = true AND n.isDeleted = false AND n.expiresAt IS NULL OR n.expiresAt > :now")
    Page<Notification> findActiveGlobalNotifications(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.isDeleted = false AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);

    List<Notification> findByExpiresAtBeforeAndIsDeletedFalse(LocalDateTime expiresAt);

    long countByExpiresAtBeforeAndIsDeletedFalse(LocalDateTime expiresAt);

    @Query("SELECT n FROM Notification n WHERE n.isDeleted = false " +
            "AND n.type = 'ALERT' AND n.category IN :categories " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentAlertsByCategories(@Param("categories") List<String> categories, Pageable pageable);
}
