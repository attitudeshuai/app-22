package com.umbrellapoint.service;

import com.umbrellapoint.dto.operation.OperationLogDto;
import com.umbrellapoint.entity.OperationLog;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public Page<OperationLogDto> getAllLogs(int page, int size, OperationLog.OperationType type,
                                            Long stationId, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OperationLog> logs;

        if (type != null && stationId != null) {
            logs = operationLogRepository.findByTypeAndStationId(type, stationId, pageable);
        } else if (type != null) {
            logs = operationLogRepository.findByType(type, pageable);
        } else if (stationId != null) {
            logs = operationLogRepository.findByStationId(stationId, pageable);
        } else {
            logs = operationLogRepository.findAll(pageable);
        }
        return logs.map(this::convertToDto);
    }

    public OperationLogDto getLogById(Long id) {
        OperationLog log = operationLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("运营日志", "id", id));
        return convertToDto(log);
    }

    @Transactional
    public OperationLogDto createLog(OperationLog.OperationType type, String description,
                                     Long relatedId, Long userId, Long stationId, Long umbrellaId,
                                     String failureReason) {
        OperationLog log = new OperationLog();
        log.setType(type);
        log.setDescription(description);
        log.setRelatedId(relatedId);
        log.setUserId(userId);
        log.setStationId(stationId);
        log.setUmbrellaId(umbrellaId);
        log.setFailureReason(failureReason);
        log = operationLogRepository.save(log);

        logger.info("运营日志已记录: type={}, description={}", type, description);
        return convertToDto(log);
    }

    @Transactional
    public void logReservationCreate(Long reservationId, Long userId, Long stationId, Long umbrellaId, String description) {
        createLog(OperationLog.OperationType.RESERVATION_CREATE, description,
                reservationId, userId, stationId, umbrellaId, null);
    }

    @Transactional
    public void logReservationComplete(Long reservationId, Long userId, Long stationId, Long umbrellaId, String description) {
        createLog(OperationLog.OperationType.RESERVATION_COMPLETE, description,
                reservationId, userId, stationId, umbrellaId, null);
    }

    @Transactional
    public void logReservationExpire(Long reservationId, Long userId, Long stationId, Long umbrellaId,
                                     String description, String failureReason) {
        createLog(OperationLog.OperationType.RESERVATION_EXPIRE, description,
                reservationId, userId, stationId, umbrellaId, failureReason);
    }

    @Transactional
    public void logReservationCancel(Long reservationId, Long userId, Long stationId, Long umbrellaId, String description) {
        createLog(OperationLog.OperationType.RESERVATION_CANCEL, description,
                reservationId, userId, stationId, umbrellaId, null);
    }

    @Transactional
    public void logUmbrellaLock(Long umbrellaId, Long stationId, Long userId, String description) {
        createLog(OperationLog.OperationType.UMBRELLA_LOCK, description,
                umbrellaId, userId, stationId, umbrellaId, null);
    }

    @Transactional
    public void logUmbrellaUnlock(Long umbrellaId, Long stationId, Long userId, String description) {
        createLog(OperationLog.OperationType.UMBRELLA_UNLOCK, description,
                umbrellaId, userId, stationId, umbrellaId, null);
    }

    @Transactional
    public void logQueueNotify(Long reservationId, Long userId, Long stationId, Long umbrellaId, String description) {
        createLog(OperationLog.OperationType.QUEUE_NOTIFY, description,
                reservationId, userId, stationId, umbrellaId, null);
    }

    private OperationLogDto convertToDto(OperationLog log) {
        return new OperationLogDto(
                log.getId(),
                log.getType(),
                log.getRelatedId(),
                log.getUserId(),
                log.getStationId(),
                log.getUmbrellaId(),
                log.getDescription(),
                log.getFailureReason(),
                log.getCreatedAt()
        );
    }
}
