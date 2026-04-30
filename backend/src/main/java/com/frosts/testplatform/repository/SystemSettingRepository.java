package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    Optional<SystemSetting> findBySettingKeyAndIsDeletedFalse(String settingKey);

    List<SystemSetting> findByIsDeletedFalseOrderByCategoryAscSortOrderAsc();

    List<SystemSetting> findByCategoryAndIsDeletedFalseOrderBySortOrderAsc(String category);

    long countByIsDeletedFalse();
}
