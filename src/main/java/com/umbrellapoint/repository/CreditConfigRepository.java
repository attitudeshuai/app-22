package com.umbrellapoint.repository;

import com.umbrellapoint.entity.CreditConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditConfigRepository extends JpaRepository<CreditConfig, Long> {
    Optional<CreditConfig> findByConfigKey(String configKey);
    List<CreditConfig> findByCategory(String category);
    boolean existsByConfigKey(String configKey);
}
