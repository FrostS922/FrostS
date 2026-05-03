package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOperatorOrderByOperatedAtDesc(String operator, Pageable pageable);

    Page<AuditLog> findByTargetOrderByOperatedAtDesc(String target, Pageable pageable);

    Page<AuditLog> findByActionOrderByOperatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findAllByOrderByOperatedAtDesc(Pageable pageable);

    List<AuditLog> findByOperatedAtBefore(LocalDateTime cutoff);
}
