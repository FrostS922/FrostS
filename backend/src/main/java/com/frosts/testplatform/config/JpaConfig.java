package com.frosts.testplatform.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.frosts.testplatform.repository")
@EntityScan(basePackages = "com.frosts.testplatform.entity")
@EnableTransactionManagement
public class JpaConfig {
}
