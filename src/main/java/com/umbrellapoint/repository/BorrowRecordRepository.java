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

    @Query("SELECT b FROM BorrowRecord b WHERE b.status IN :statuses AND b.appealStatus <> :appealStatus AND b.borrowTime < :thresholdTime")
    List<BorrowRecord> findByStatusInAndAppealStatusNotAndBorrowTimeBefore(
            List<BorrowRecord.BorrowStatus> statuses,
            BorrowRecord.AppealStatus appealStatus,
            LocalDateTime thresholdTime);

    List<BorrowRecord> findByAppealStatus(BorrowRecord.AppealStatus appealStatus);

    Page<BorrowRecord> findByAppealStatus(BorrowRecord.AppealStatus appealStatus, Pageable pageable);

    long countByAppealStatus(BorrowRecord.AppealStatus appealStatus);

    long countByBorrowStationId(Long borrowStationId);

    long countByReturnStationId(Long returnStationId);

    long countByBorrowStationIdAndBorrowTimeBetween(Long borrowStationId, LocalDateTime startTime, LocalDateTime endTime);

    long countByReturnStationIdAndReturnTimeBetween(Long returnStationId, LocalDateTime startTime, LocalDateTime endTime);

    long countByBorrowStationIdAndStatusAndBorrowTimeBetween(Long borrowStationId, BorrowRecord.BorrowStatus status, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT b.borrowStationId, COUNT(b) FROM BorrowRecord b WHERE b.borrowTime BETWEEN :startTime AND :endTime GROUP BY b.borrowStationId")
    List<Object[]> countBorrowsByStationAndTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT b.returnStationId, COUNT(b) FROM BorrowRecord b WHERE b.returnTime BETWEEN :startTime AND :endTime AND b.returnStationId IS NOT NULL GROUP BY b.returnStationId")
    List<Object[]> countReturnsByStationAndTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT b.borrowStationId, COUNT(b) FROM BorrowRecord b WHERE b.status = :status AND b.borrowTime BETWEEN :startTime AND :endTime GROUP BY b.borrowStationId")
    List<Object[]> countOverdueByBorrowStationAndTimeRange(BorrowRecord.BorrowStatus status, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT b.borrowStationId, AVG(TIMESTAMPDIFF(MINUTE, b.borrowTime, b.returnTime)) FROM BorrowRecord b WHERE b.status = :status AND b.returnTime IS NOT NULL AND b.borrowTime BETWEEN :startTime AND :endTime GROUP BY b.borrowStationId")
    List<Object[]> avgBorrowDurationByBorrowStationAndTimeRange(BorrowRecord.BorrowStatus status, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT COUNT(b) FROM BorrowRecord b WHERE b.borrowStationId = :stationId AND b.status = :status AND b.borrowTime BETWEEN :startTime AND :endTime")
    long countOverdueByStationIdAndTimeRange(Long stationId, BorrowRecord.BorrowStatus status, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, b.borrowTime, b.returnTime)) FROM BorrowRecord b WHERE b.borrowStationId = :stationId AND b.status = :status AND b.returnTime IS NOT NULL AND b.borrowTime BETWEEN :startTime AND :endTime")
    Double avgBorrowDurationByStationIdAndTimeRange(Long stationId, BorrowRecord.BorrowStatus status, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT COUNT(b) FROM BorrowRecord b WHERE b.borrowStationId = :stationId AND b.isCrossRegion = true AND b.borrowTime BETWEEN :startTime AND :endTime")
    long countCrossRegionByStationIdAndTimeRange(Long stationId, LocalDateTime startTime, LocalDateTime endTime);
}
