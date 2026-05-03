package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    Page<NotificationRecipient> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<NotificationRecipient> findByUserIdAndIsReadAndIsDeletedFalse(Long userId, Boolean isRead, Pageable pageable);

    @Query("SELECT nr FROM NotificationRecipient nr JOIN FETCH nr.notification n " +
            "WHERE nr.user.id = :userId AND nr.isDeleted = false " +
            "AND (:type IS NULL OR n.type = :type) " +
            "AND (:isRead IS NULL OR nr.isRead = :isRead) " +
            "ORDER BY n.createdAt DESC")
    Page<NotificationRecipient> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    List<NotificationRecipient> findByNotificationIdAndIsDeletedFalse(Long notificationId);

    @Modifying
    @Query("UPDATE NotificationRecipient nr SET nr.isRead = true, nr.readAt = CURRENT_TIMESTAMP WHERE nr.user.id = :userId AND nr.isRead = false AND nr.isDeleted = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);
}
