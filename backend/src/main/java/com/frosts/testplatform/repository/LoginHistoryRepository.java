package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUsernameOrderByLoginAtDesc(String username, Pageable pageable);

    long countByUsername(String username);

    List<LoginHistory> findByLoginAtBefore(LocalDateTime cutoff);

    List<LoginHistory> findByLoginAtBetween(LocalDateTime start, LocalDateTime end);
}
