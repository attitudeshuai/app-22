package com.umbrellapoint.repository;

import com.umbrellapoint.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByType(OperationLog.OperationType type, Pageable pageable);

    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    Page<OperationLog> findByStationId(Long stationId, Pageable pageable);

    Page<OperationLog> findByRelatedIdAndType(Long relatedId, OperationLog.OperationType type, Pageable pageable);

    List<OperationLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Page<OperationLog> findByTypeAndStationId(OperationLog.OperationType type, Long stationId, Pageable pageable);
}
