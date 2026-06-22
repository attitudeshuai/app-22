package com.umbrellapoint.repository;

import com.umbrellapoint.entity.BorrowRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    Page<BorrowRecord> findByUserId(Long userId, Pageable pageable);
    Page<BorrowRecord> findByStatus(BorrowRecord.BorrowStatus status, Pageable pageable);
    Page<BorrowRecord> findByUmbrellaId(Long umbrellaId, Pageable pageable);
    List<BorrowRecord> findByStatusAndBorrowTimeBefore(BorrowRecord.BorrowStatus status, LocalDateTime time);
    long countByStatus(BorrowRecord.BorrowStatus status);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, BorrowRecord.BorrowStatus status);
    long countByBorrowStationIdAndStatus(Long borrowStationId, BorrowRecord.BorrowStatus status);
    long countByUserIdAndStatus(Long userId, BorrowRecord.BorrowStatus status);

    @Query("SELECT DATE(b.borrowTime) as date, COUNT(b) as count FROM BorrowRecord b WHERE b.borrowTime BETWEEN :start AND :end GROUP BY DATE(b.borrowTime)")
    List<Object[]> countByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM BorrowRecord b WHERE b.status = :status AND b.appealStatus <> :appealStatus AND b.borrowTime < :thresholdTime")
    List<BorrowRecord> findByStatusAndAppealStatusNotAndBorrowTimeBefore(
            BorrowRecord.BorrowStatus status,
            BorrowRecord.AppealStatus appealStatus,
            LocalDateTime thresholdTime);

    List<BorrowRecord> findByAppealStatus(BorrowRecord.AppealStatus appealStatus);

    Page<BorrowRecord> findByAppealStatus(BorrowRecord.AppealStatus appealStatus, Pageable pageable);

    long countByAppealStatus(BorrowRecord.AppealStatus appealStatus);
}
