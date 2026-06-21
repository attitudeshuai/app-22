package com.umbrellapoint.repository;

import com.umbrellapoint.entity.UserCredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {
    Optional<UserCredit> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Page<UserCredit> findByScoreLessThan(Integer score, Pageable pageable);
}
