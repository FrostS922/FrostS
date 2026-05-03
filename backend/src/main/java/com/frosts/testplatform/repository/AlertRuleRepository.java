package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByEnabledTrueAndIsDeletedFalse();

    List<AlertRule> findByRuleTypeAndEnabledTrueAndIsDeletedFalse(String ruleType);

    List<AlertRule> findByIsDeletedFalseOrderByCreatedAtDesc();
}
