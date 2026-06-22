package com.umbrellapoint.repository;

import com.umbrellapoint.entity.CreditChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditChangeLogRepository extends JpaRepository<CreditChangeLog, Long> {
    Page<CreditChangeLog> findByUserId(Long userId, Pageable pageable);
    Page<CreditChangeLog> findByChangeType(CreditChangeLog.ChangeType changeType, Pageable pageable);
    Page<CreditChangeLog> findByUserIdAndChangeType(Long userId, CreditChangeLog.ChangeType changeType, Pageable pageable);
    List<CreditChangeLog> findByBorrowRecordId(Long borrowRecordId);

    @Query("SELECT c FROM CreditChangeLog c WHERE c.userId = :userId AND c.borrowRecordId = :borrowRecordId AND c.changeType = :changeType AND c.createdAt >= :startOfDay")
    Optional<CreditChangeLog> findTodayPenaltyByUserAndRecord(
            @Param("userId") Long userId,
            @Param("borrowRecordId") Long borrowRecordId,
            @Param("changeType") CreditChangeLog.ChangeType changeType,
            @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(c) > 0 FROM CreditChangeLog c WHERE c.userId = :userId AND c.borrowRecordId = :borrowRecordId AND c.changeType = :changeType AND FUNCTION('DATE', c.createdAt) = FUNCTION('DATE', :dateTime)")
    boolean existsPenaltyForRecordOnDate(
            @Param("userId") Long userId,
            @Param("borrowRecordId") Long borrowRecordId,
            @Param("changeType") CreditChangeLog.ChangeType changeType,
            @Param("dateTime") LocalDateTime dateTime);
}
