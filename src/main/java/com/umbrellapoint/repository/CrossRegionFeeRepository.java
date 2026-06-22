package com.umbrellapoint.repository;

import com.umbrellapoint.entity.CrossRegionFee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrossRegionFeeRepository extends JpaRepository<CrossRegionFee, Long> {

    Optional<CrossRegionFee> findByBorrowRecordId(Long borrowRecordId);

    Page<CrossRegionFee> findByUserId(Long userId, Pageable pageable);

    List<CrossRegionFee> findByUserIdAndStatus(Long userId, CrossRegionFee.FeeStatus status);

    Page<CrossRegionFee> findByUserIdAndStatus(Long userId, CrossRegionFee.FeeStatus status, Pageable pageable);

    Page<CrossRegionFee> findByStatus(CrossRegionFee.FeeStatus status, Pageable pageable);

    BigDecimal sumFeeAmountByUserIdAndStatus(Long userId, CrossRegionFee.FeeStatus status);

    Integer countByUserIdAndStatus(Long userId, CrossRegionFee.FeeStatus status);

    List<CrossRegionFee> findByStatusAndSettlementDueDateBefore(CrossRegionFee.FeeStatus status, LocalDateTime dateTime);

    boolean existsByBorrowRecordId(Long borrowRecordId);
}
