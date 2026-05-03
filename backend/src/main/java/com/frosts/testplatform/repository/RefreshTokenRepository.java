package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndIsRevokedFalse(String token);

    List<RefreshToken> findByUsernameAndIsRevokedFalse(String username);

    void deleteByUsername(String username);

    void deleteByExpiryDateBefore(LocalDateTime now);

    List<RefreshToken> findByUsernameAndIsRevokedFalseAndExpiryDateAfter(String username, LocalDateTime now);

    List<RefreshToken> findByExpiryDateAfterAndIsRevokedFalse(LocalDateTime now);
}
