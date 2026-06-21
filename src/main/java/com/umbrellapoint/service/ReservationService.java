package com.umbrellapoint.service;

import com.umbrellapoint.dto.reservation.ReservationDto;
import com.umbrellapoint.dto.reservation.ReservationRequest;
import com.umbrellapoint.entity.*;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final int DEFAULT_EXPIRE_MINUTES = 30;
    private static final int MIN_CREDIT_SCORE = 60;
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("29.90");

    private final ReservationRepository reservationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final StationRepository stationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;
    private final OperationLogService operationLogService;

    public ReservationService(ReservationRepository reservationRepository,
                              UmbrellaRepository umbrellaRepository,
                              StationRepository stationRepository,
                              BorrowRecordRepository borrowRecordRepository,
                              UserCreditRepository userCreditRepository,
                              AuthService authService,
                              OperationLogService operationLogService) {
        this.reservationRepository = reservationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.stationRepository = stationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
        this.authService = authService;
        this.operationLogService = operationLogService;
    }

    public Page<ReservationDto> getAllReservations(int page, int size, Long stationId,
                                                    Reservation.ReservationStatus status,
                                                    String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Reservation> reservations;

        if (stationId != null) {
            reservations = reservationRepository.findByStationId(stationId, pageable);
        } else if (status != null) {
            reservations = reservationRepository.findByStatus(status, pageable);
        } else {
            reservations = reservationRepository.findAll(pageable);
        }
        return reservations.map(this::convertToDto);
    }

    public Page<ReservationDto> getMyReservations(int page, int size, String sortBy, String sortDir) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return reservationRepository.findByUserId(currentUserId, pageable)
                .map(this::convertToDto);
    }

    public ReservationDto getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("预约记录", "id", id));
        return convertToDto(reservation);
    }

    @Transactional
    public ReservationDto createReservation(ReservationRequest request) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        boolean hasActiveReservation = reservationRepository.existsByUserIdAndStatusIn(
                currentUserId,
                Arrays.asList(Reservation.ReservationStatus.Active, Reservation.ReservationStatus.Pending)
        );
        if (hasActiveReservation) {
            throw new BusinessException("您已有进行中的预约，请先完成或取消后再预约");
        }

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("借还点", "id", request.getStationId()));

        if (!Boolean.TRUE.equals(station.getIsActive())) {
            throw new BusinessException("该借还点已停用，暂时无法预约");
        }

        UserCredit credit = userCreditRepository.findByUserId(currentUserId).orElse(null);
        if (credit == null || credit.getScore() < MIN_CREDIT_SCORE) {
            int score = credit != null ? credit.getScore() : 0;
            throw new BusinessException("信用分不足，最低需要" + MIN_CREDIT_SCORE + "分才能预约");
        }

        List<BorrowRecord> ongoingRecords = borrowRecordRepository.findByUserIdAndStatus(
                currentUserId, BorrowRecord.BorrowStatus.Ongoing);
        if (!ongoingRecords.isEmpty()) {
            throw new BusinessException("您有未归还的雨伞，请先归还后再预约");
        }

        List<Umbrella> availableUmbrellas = umbrellaRepository.findByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);

        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setStationId(station.getId());
        reservation.setExpectedBorrowStart(request.getExpectedBorrowStart());
        reservation.setExpectedBorrowEnd(request.getExpectedBorrowEnd());

        LocalDateTime expireTime = calculateExpireTime(request.getExpectedBorrowStart());
        reservation.setExpireTime(expireTime);

        if (!availableUmbrellas.isEmpty()) {
            Umbrella umbrella = availableUmbrellas.get(0);
            umbrella.setStatus(Umbrella.UmbrellaStatus.Reserved);
            umbrellaRepository.save(umbrella);

            reservation.setUmbrellaId(umbrella.getId());
            reservation.setStatus(Reservation.ReservationStatus.Active);
            reservation.setQueuePosition(0);

            operationLogService.logUmbrellaLock(umbrella.getId(), station.getId(), currentUserId,
                    "预约锁定雨伞: " + umbrella.getCode());

            logger.info("预约成功: userId={}, stationId={}, umbrellaId={}", currentUserId, station.getId(), umbrella.getId());
        } else {
            long pendingCount = reservationRepository.countByStationIdAndStatus(
                    station.getId(), Reservation.ReservationStatus.Pending);
            reservation.setStatus(Reservation.ReservationStatus.Pending);
            reservation.setQueuePosition((int) pendingCount + 1);

            logger.info("已加入排队: userId={}, stationId={}, position={}", currentUserId, station.getId(), pendingCount + 1);
        }

        reservation = reservationRepository.save(reservation);

        operationLogService.logReservationCreate(reservation.getId(), currentUserId, station.getId(),
                reservation.getUmbrellaId(),
                reservation.getStatus() == Reservation.ReservationStatus.Active ? "预约成功，雨伞已锁定" : "预约成功，已加入排队");

        return convertToDto(reservation);
    }

    @Transactional
    public ReservationDto cancelReservation(Long id) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("预约记录", "id", id));

        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权取消他人的预约");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.Active
                && reservation.getStatus() != Reservation.ReservationStatus.Pending) {
            throw new BusinessException("该预约状态不允许取消");
        }

        reservation.setStatus(Reservation.ReservationStatus.Cancelled);
        reservation.setCompletedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        if (reservation.getUmbrellaId() != null) {
            Umbrella umbrella = umbrellaRepository.findById(reservation.getUmbrellaId()).orElse(null);
            if (umbrella != null && umbrella.getStatus() == Umbrella.UmbrellaStatus.Reserved) {
                umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
                umbrellaRepository.save(umbrella);

                operationLogService.logUmbrellaUnlock(umbrella.getId(), reservation.getStationId(), currentUserId,
                        "取消预约释放雨伞: " + umbrella.getCode());
            }
        }

        if (reservation.getStatus() == Reservation.ReservationStatus.Active) {
            notifyNextInQueue(reservation.getStationId());
        }

        operationLogService.logReservationCancel(reservation.getId(), currentUserId,
                reservation.getStationId(), reservation.getUmbrellaId(), "用户主动取消预约");

        updateQueuePositions(reservation.getStationId());

        logger.info("预约已取消: reservationId={}, userId={}", id, currentUserId);
        return convertToDto(reservation);
    }

    @Transactional
    public BorrowRecord confirmPickup(Long reservationId) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("预约记录", "id", reservationId));

        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权使用他人的预约");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.Active) {
            throw new BusinessException("该预约状态不可取伞");
        }

        if (LocalDateTime.now().isAfter(reservation.getExpireTime())) {
            throw new BusinessException("预约已过期，请重新预约");
        }

        Umbrella umbrella = umbrellaRepository.findById(reservation.getUmbrellaId())
                .orElseThrow(() -> new ResourceNotFoundException("雨伞", "id", reservation.getUmbrellaId()));

        if (umbrella.getStatus() != Umbrella.UmbrellaStatus.Reserved) {
            throw new BusinessException("雨伞状态异常，无法取伞");
        }

        umbrella.setStatus(Umbrella.UmbrellaStatus.Borrowed);
        umbrellaRepository.save(umbrella);

        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUmbrellaId(umbrella.getId());
        borrowRecord.setUserId(currentUserId);
        borrowRecord.setBorrowStationId(reservation.getStationId());
        borrowRecord.setBorrowTime(LocalDateTime.now());
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.Ongoing);
        borrowRecord.setDeposit(DEFAULT_DEPOSIT);
        borrowRecord = borrowRecordRepository.save(borrowRecord);

        reservation.setStatus(Reservation.ReservationStatus.Completed);
        reservation.setBorrowRecordId(borrowRecord.getId());
        reservation.setCompletedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        operationLogService.logReservationComplete(reservation.getId(), currentUserId,
                reservation.getStationId(), umbrella.getId(), "预约取伞成功，已生成正式借还记录");

        logger.info("预约取伞成功: reservationId={}, userId={}, umbrellaId={}, borrowRecordId={}",
                reservationId, currentUserId, umbrella.getId(), borrowRecord.getId());

        return borrowRecord;
    }

    @Transactional
    public void processExpiredReservations() {
        logger.info("开始检查过期预约...");

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndExpireTimeBefore(Reservation.ReservationStatus.Active, LocalDateTime.now());

        int count = 0;
        for (Reservation reservation : expiredReservations) {
            expireReservation(reservation);
            count++;
        }

        logger.info("过期预约检查完成，共处理 {} 条过期预约", count);
    }

    @Transactional
    public void expireReservation(Reservation reservation) {
        reservation.setStatus(Reservation.ReservationStatus.Expired);
        reservation.setCompletedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        if (reservation.getUmbrellaId() != null) {
            Umbrella umbrella = umbrellaRepository.findById(reservation.getUmbrellaId()).orElse(null);
            if (umbrella != null && umbrella.getStatus() == Umbrella.UmbrellaStatus.Reserved) {
                umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
                umbrellaRepository.save(umbrella);

                operationLogService.logUmbrellaUnlock(umbrella.getId(), reservation.getStationId(),
                        reservation.getUserId(), "预约过期释放雨伞: " + umbrella.getCode());
            }
        }

        operationLogService.logReservationExpire(reservation.getId(), reservation.getUserId(),
                reservation.getStationId(), reservation.getUmbrellaId(),
                "预约已过期，用户未在有效期内取伞",
                "超过预约有效期未取伞，预约自动失效");

        notifyNextInQueue(reservation.getStationId());
        updateQueuePositions(reservation.getStationId());

        logger.info("预约已过期: reservationId={}, userId={}", reservation.getId(), reservation.getUserId());
    }

    @Transactional
    public void notifyNextInQueue(Long stationId) {
        Reservation nextReservation = reservationRepository
                .findFirstByStationIdAndStatusOrderByCreatedAtAsc(stationId, Reservation.ReservationStatus.Pending)
                .orElse(null);

        if (nextReservation == null) {
            return;
        }

        List<Umbrella> availableUmbrellas = umbrellaRepository.findByStationIdAndStatus(
                stationId, Umbrella.UmbrellaStatus.Available);

        if (availableUmbrellas.isEmpty()) {
            return;
        }

        Umbrella umbrella = availableUmbrellas.get(0);
        umbrella.setStatus(Umbrella.UmbrellaStatus.Reserved);
        umbrellaRepository.save(umbrella);

        nextReservation.setUmbrellaId(umbrella.getId());
        nextReservation.setStatus(Reservation.ReservationStatus.Active);
        nextReservation.setQueuePosition(0);
        nextReservation.setExpireTime(LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES));
        reservationRepository.save(nextReservation);

        operationLogService.logUmbrellaLock(umbrella.getId(), stationId, nextReservation.getUserId(),
                "排队预约锁定雨伞: " + umbrella.getCode());

        operationLogService.logQueueNotify(nextReservation.getId(), nextReservation.getUserId(),
                stationId, umbrella.getId(), "排队预约已生效，雨伞已锁定，请及时取伞");

        updateQueuePositions(stationId);

        logger.info("排队预约已生效: reservationId={}, userId={}, umbrellaId={}",
                nextReservation.getId(), nextReservation.getUserId(), umbrella.getId());
    }

    private void updateQueuePositions(Long stationId) {
        List<Reservation> pendingReservations = reservationRepository
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, Reservation.ReservationStatus.Pending);

        int position = 1;
        for (Reservation reservation : pendingReservations) {
            reservation.setQueuePosition(position);
            reservationRepository.save(reservation);
            position++;
        }
    }

    private LocalDateTime calculateExpireTime(LocalDateTime expectedBorrowStart) {
        if (expectedBorrowStart != null && expectedBorrowStart.isAfter(LocalDateTime.now())) {
            return expectedBorrowStart.plusMinutes(DEFAULT_EXPIRE_MINUTES);
        }
        return LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES);
    }

    private ReservationDto convertToDto(Reservation reservation) {
        Station station = stationRepository.findById(reservation.getStationId()).orElse(null);
        Umbrella umbrella = reservation.getUmbrellaId() != null
                ? umbrellaRepository.findById(reservation.getUmbrellaId()).orElse(null)
                : null;

        return new ReservationDto(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getUmbrellaId(),
                reservation.getStationId(),
                station != null ? station.getName() : null,
                umbrella != null ? umbrella.getCode() : null,
                reservation.getExpectedBorrowStart(),
                reservation.getExpectedBorrowEnd(),
                reservation.getExpireTime(),
                reservation.getBorrowRecordId(),
                reservation.getStatus(),
                reservation.getQueuePosition(),
                reservation.getCreatedAt(),
                reservation.getCompletedAt()
        );
    }

    public Reservation findActiveReservationByUserAndStation(Long userId, Long stationId) {
        List<Reservation> activeReservations = reservationRepository
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, Reservation.ReservationStatus.Active);
        for (Reservation reservation : activeReservations) {
            if (reservation.getUserId().equals(userId)) {
                return reservation;
            }
        }
        return null;
    }
}
