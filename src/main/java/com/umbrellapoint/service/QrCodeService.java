package com.umbrellapoint.service;

import com.umbrellapoint.dto.qrcode.ScanBorrowRequest;
import com.umbrellapoint.dto.qrcode.ScanBorrowResponse;
import com.umbrellapoint.dto.qrcode.ScanReturnRequest;
import com.umbrellapoint.dto.qrcode.ScanReturnResponse;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.StationRepository;
import com.umbrellapoint.repository.UmbrellaRepository;
import com.umbrellapoint.repository.UserCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);
    private static final int MIN_CREDIT_SCORE = 60;
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("29.90");

    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;
    private final ReservationService reservationService;

    public QrCodeService(StationRepository stationRepository,
                         UmbrellaRepository umbrellaRepository,
                         BorrowRecordRepository borrowRecordRepository,
                         UserCreditRepository userCreditRepository,
                         AuthService authService,
                         ReservationService reservationService) {
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
        this.authService = authService;
        this.reservationService = reservationService;
    }

    @Transactional
    public ScanBorrowResponse scanBorrow(ScanBorrowRequest request) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Station station = stationRepository.findByQrCode(request.getQrCode())
                .orElseThrow(() -> {
                    logger.warn("扫码借伞失败: 无效二维码, qrCode={}, userId={}", request.getQrCode(), currentUserId);
                    return new BusinessException("二维码无效，未识别到对应借还点");
                });

        if (!Boolean.TRUE.equals(station.getIsActive())) {
            logger.warn("扫码借伞失败: 站点已停用, stationId={}, userId={}", station.getId(), currentUserId);
            throw new BusinessException("该借还点已停用，暂时无法借伞");
        }

        UserCredit credit = userCreditRepository.findByUserId(currentUserId).orElse(null);
        if (credit == null || credit.getScore() < MIN_CREDIT_SCORE) {
            int score = credit != null ? credit.getScore() : 0;
            logger.warn("扫码借伞失败: 信用分不足, userId={}, score={}, threshold={}", currentUserId, score, MIN_CREDIT_SCORE);
            throw new BusinessException("信用分不足，最低需要" + MIN_CREDIT_SCORE + "分才能借伞");
        }

        List<BorrowRecord> ongoingRecords = borrowRecordRepository.findByUserIdAndStatus(
                currentUserId, BorrowRecord.BorrowStatus.Ongoing);
        if (!ongoingRecords.isEmpty()) {
            logger.warn("扫码借伞失败: 用户有进行中的借还记录, userId={}", currentUserId);
            throw new BusinessException("您有未归还的雨伞，请先归还后再借");
        }

        com.umbrellapoint.entity.Reservation activeReservation = reservationService
                .findActiveReservationByUserAndStation(currentUserId, station.getId());

        if (activeReservation != null) {
            return borrowWithReservation(activeReservation, station);
        }

        return borrowWithoutReservation(station, currentUserId);
    }

    private ScanBorrowResponse borrowWithReservation(com.umbrellapoint.entity.Reservation reservation, Station station) {
        BorrowRecord borrowRecord = reservationService.confirmPickup(reservation.getId());

        Umbrella umbrella = umbrellaRepository.findById(borrowRecord.getUmbrellaId()).orElse(null);

        long availableCount = umbrellaRepository.countByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);
        boolean restockAlert = checkAndNotifyRestock(station, (int) availableCount);

        logger.info("预约扫码借伞成功: userId={}, umbrellaId={}, stationId={}, recordId={}, reservationId={}",
                reservation.getUserId(), borrowRecord.getUmbrellaId(), station.getId(),
                borrowRecord.getId(), reservation.getId());

        return new ScanBorrowResponse(
                borrowRecord.getId(),
                borrowRecord.getUmbrellaId(),
                umbrella != null ? umbrella.getCode() : null,
                station.getId(),
                station.getName(),
                borrowRecord.getBorrowTime(),
                borrowRecord.getDeposit(),
                borrowRecord.getStatus(),
                (int) availableCount,
                restockAlert
        );
    }

    private ScanBorrowResponse borrowWithoutReservation(Station station, Long userId) {
        List<Umbrella> availableUmbrellas = umbrellaRepository.findByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);
        if (availableUmbrellas.isEmpty()) {
            logger.warn("扫码借伞失败: 站点无可用雨伞, stationId={}, userId={}", station.getId(), userId);
            throw new BusinessException("该借还点暂无可用雨伞，您可以先进行预约排队");
        }

        Umbrella umbrella = availableUmbrellas.get(0);
        umbrella.setStatus(Umbrella.UmbrellaStatus.Borrowed);
        umbrellaRepository.save(umbrella);

        BorrowRecord record = new BorrowRecord();
        record.setUmbrellaId(umbrella.getId());
        record.setUserId(userId);
        record.setBorrowStationId(station.getId());
        record.setBorrowTime(LocalDateTime.now());
        record.setStatus(BorrowRecord.BorrowStatus.Ongoing);
        record.setDeposit(DEFAULT_DEPOSIT);
        record = borrowRecordRepository.save(record);

        long availableCount = umbrellaRepository.countByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);
        boolean restockAlert = checkAndNotifyRestock(station, (int) availableCount);

        logger.info("扫码借伞成功: userId={}, umbrellaId={}, stationId={}, recordId={}",
                userId, umbrella.getId(), station.getId(), record.getId());

        return new ScanBorrowResponse(
                record.getId(),
                umbrella.getId(),
                umbrella.getCode(),
                station.getId(),
                station.getName(),
                record.getBorrowTime(),
                record.getDeposit(),
                record.getStatus(),
                (int) availableCount,
                restockAlert
        );
    }

    @Transactional
    public ScanReturnResponse scanReturn(ScanReturnRequest request) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Station station = stationRepository.findByQrCode(request.getQrCode())
                .orElseThrow(() -> {
                    logger.warn("扫码还伞失败: 无效二维码, qrCode={}, userId={}", request.getQrCode(), currentUserId);
                    return new BusinessException("二维码无效，未识别到对应借还点");
                });

        if (!Boolean.TRUE.equals(station.getIsActive())) {
            logger.warn("扫码还伞失败: 站点已停用, stationId={}, userId={}", station.getId(), currentUserId);
            throw new BusinessException("该借还点已停用，暂时无法还伞");
        }

        long currentCount = umbrellaRepository.countByStationId(station.getId());
        if (currentCount >= station.getCapacity()) {
            logger.warn("扫码还伞失败: 站点已满, stationId={}, userId={}", station.getId(), currentUserId);
            throw new BusinessException("该借还点已满，请选择其他借还点还伞");
        }

        List<BorrowRecord> ongoingRecords = borrowRecordRepository.findByUserIdAndStatus(
                currentUserId, BorrowRecord.BorrowStatus.Ongoing);
        if (ongoingRecords.isEmpty()) {
            logger.warn("扫码还伞失败: 用户无进行中的借还记录, userId={}", currentUserId);
            throw new BusinessException("您没有需要归还的雨伞");
        }

        BorrowRecord record = ongoingRecords.get(0);
        record.setStatus(BorrowRecord.BorrowStatus.Returned);
        record.setReturnTime(LocalDateTime.now());
        record.setReturnStationId(station.getId());

        Umbrella umbrella = umbrellaRepository.findById(record.getUmbrellaId()).orElse(null);
        if (umbrella != null) {
            umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
            umbrella.setStationId(station.getId());
            umbrellaRepository.save(umbrella);
        }

        record = borrowRecordRepository.save(record);

        long availableCount = umbrellaRepository.countByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);
        boolean restockAlert = checkAndNotifyRestock(station, (int) availableCount);

        Station borrowStation = stationRepository.findById(record.getBorrowStationId()).orElse(null);

        logger.info("扫码还伞成功: userId={}, umbrellaId={}, returnStationId={}, recordId={}",
                currentUserId, record.getUmbrellaId(), station.getId(), record.getId());

        return new ScanReturnResponse(
                record.getId(),
                record.getUmbrellaId(),
                umbrella != null ? umbrella.getCode() : null,
                record.getBorrowStationId(),
                borrowStation != null ? borrowStation.getName() : null,
                station.getId(),
                station.getName(),
                record.getBorrowTime(),
                record.getReturnTime(),
                record.getDeposit(),
                record.getStatus(),
                (int) availableCount,
                restockAlert
        );
    }

    private boolean checkAndNotifyRestock(Station station, int availableCount) {
        int threshold = station.getSafetyThreshold() != null ? station.getSafetyThreshold() : 5;
        if (availableCount < threshold) {
            logger.warn("库存低于安全线: stationId={}, stationName={}, availableCount={}, threshold={}",
                    station.getId(), station.getName(), availableCount, threshold);
            notifyNearestOperator(station, availableCount, threshold);
            return true;
        }
        return false;
    }

    private void notifyNearestOperator(Station station, int availableCount, int threshold) {
        logger.info("触发补货提醒: stationId={}, stationName={}, availableCount={}, threshold={}, managerId={}",
                station.getId(), station.getName(), availableCount, threshold, station.getManagerId());
    }
}
