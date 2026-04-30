package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.isDeleted = false
              AND (
                LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.realName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<User> searchActiveUsers(@Param("search") String search, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByIsDeletedFalse();

    long countByEnabledAndIsDeletedFalse(Boolean enabled);

    long countByRoles_IdAndIsDeletedFalse(Long roleId);
}
