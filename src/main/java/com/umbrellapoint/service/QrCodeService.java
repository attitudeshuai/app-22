package com.umbrellapoint.service;

import com.umbrellapoint.dto.fee.CrossRegionFeeDto;
import com.umbrellapoint.dto.qrcode.ScanBorrowRequest;
import com.umbrellapoint.dto.qrcode.ScanBorrowResponse;
import com.umbrellapoint.dto.qrcode.ScanReturnRequest;
import com.umbrellapoint.dto.qrcode.ScanReturnResponse;
import com.umbrellapoint.dto.station.NearbyStationDto;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.CrossRegionFee;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.entity.Umbrella;
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
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("29.90");

    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;
    private final ReservationService reservationService;
    private final CreditValidationService creditValidationService;
    private final CrossRegionFeeService crossRegionFeeService;
    private final StationService stationService;

    public QrCodeService(StationRepository stationRepository,
                         UmbrellaRepository umbrellaRepository,
                         BorrowRecordRepository borrowRecordRepository,
                         UserCreditRepository userCreditRepository,
                         AuthService authService,
                         ReservationService reservationService,
                         CreditValidationService creditValidationService,
                         CrossRegionFeeService crossRegionFeeService,
                         StationService stationService) {
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
        this.authService = authService;
        this.reservationService = reservationService;
        this.creditValidationService = creditValidationService;
        this.crossRegionFeeService = crossRegionFeeService;
        this.stationService = stationService;
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

        creditValidationService.validateBorrowPermission(currentUserId, station.getId(), null);

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

        Station returnStation = stationRepository.findByQrCode(request.getQrCode())
                .orElseThrow(() -> {
                    logger.warn("扫码还伞失败: 无效二维码, qrCode={}, userId={}", request.getQrCode(), currentUserId);
                    return new BusinessException("二维码无效，未识别到对应借还点");
                });

        if (!Boolean.TRUE.equals(returnStation.getIsActive())) {
            logger.warn("扫码还伞失败: 站点已停用, stationId={}, userId={}", returnStation.getId(), currentUserId);
            throw new BusinessException("该借还点已停用，暂时无法还伞");
        }

        List<BorrowRecord> ongoingRecords = borrowRecordRepository.findByUserIdAndStatus(
                currentUserId, BorrowRecord.BorrowStatus.Ongoing);
        if (ongoingRecords.isEmpty()) {
            logger.warn("扫码还伞失败: 用户无进行中的借还记录, userId={}", currentUserId);
            throw new BusinessException("您没有需要归还的雨伞");
        }

        BorrowRecord record = ongoingRecords.get(0);
        Long borrowStationId = record.getBorrowStationId();
        Station borrowStation = stationRepository.findById(borrowStationId).orElse(null);

        boolean isCrossRegion = crossRegionFeeService.isCrossRegion(borrowStationId, returnStation.getId());

        long currentCount = umbrellaRepository.countByStationId(returnStation.getId());
        boolean capacityOverflow = currentCount >= returnStation.getCapacity();

        if (capacityOverflow) {
            List<NearbyStationDto> nearbyStations = stationService.findNearbyReturnStations(returnStation);
            logger.warn("扫码还伞: 站点容量已满, stationId={}, userId={}, 查找附近站点数={}",
                    returnStation.getId(), currentUserId, nearbyStations.size());

            ScanReturnResponse response = new ScanReturnResponse();
            response.setRecordId(record.getId());
            response.setUmbrellaId(record.getUmbrellaId());
            Umbrella umbrella = umbrellaRepository.findById(record.getUmbrellaId()).orElse(null);
            if (umbrella != null) {
                response.setUmbrellaCode(umbrella.getCode());
            }
            response.setBorrowStationId(borrowStationId);
            response.setBorrowStationName(borrowStation != null ? borrowStation.getName() : null);
            response.setReturnStationId(returnStation.getId());
            response.setReturnStationName(returnStation.getName());
            response.setBorrowTime(record.getBorrowTime());
            response.setStatus(record.getStatus());
            response.setDeposit(record.getDeposit());
            response.setIsCrossRegion(isCrossRegion);
            response.setCapacityOverflow(true);
            response.setNearbyStations(nearbyStations);
            response.setPaymentStatus(record.getPaymentStatus());

            if (isCrossRegion) {
                BigDecimal distance = crossRegionFeeService.calculateDistanceKm(borrowStation, returnStation);
                BigDecimal estimatedFee = crossRegionFeeService.calculateFee(distance);
                CrossRegionFeeDto feeInfo = new CrossRegionFeeDto();
                feeInfo.setBorrowRecordId(record.getId());
                feeInfo.setBorrowStationId(borrowStationId);
                feeInfo.setBorrowStationName(borrowStation != null ? borrowStation.getName() : null);
                feeInfo.setReturnStationId(returnStation.getId());
                feeInfo.setReturnStationName(returnStation.getName());
                feeInfo.setDistanceKm(distance);
                feeInfo.setFeeAmount(estimatedFee);
                response.setCrossRegionFee(feeInfo);
            }

            return response;
        }

        record.setStatus(BorrowRecord.BorrowStatus.Returned);
        record.setReturnTime(LocalDateTime.now());
        record.setReturnStationId(returnStation.getId());
        if (!isCrossRegion) {
            record.setIsCrossRegion(false);
            record.setPaymentStatus(BorrowRecord.PaymentStatus.None);
        }

        Umbrella umbrella = umbrellaRepository.findById(record.getUmbrellaId()).orElse(null);
        if (umbrella != null) {
            umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
            umbrella.setStationId(returnStation.getId());
            umbrellaRepository.save(umbrella);
        }

        record = borrowRecordRepository.save(record);

        CrossRegionFee crossRegionFee = null;
        CrossRegionFeeDto feeDto = null;
        if (isCrossRegion && borrowStation != null) {
            crossRegionFee = crossRegionFeeService.createCrossRegionFee(record, borrowStation, returnStation);
            if (crossRegionFee != null) {
                feeDto = crossRegionFeeService.convertToDto(crossRegionFee);
            }
        }

        long availableCount = umbrellaRepository.countByStationIdAndStatus(
                returnStation.getId(), Umbrella.UmbrellaStatus.Available);
        boolean restockAlert = checkAndNotifyRestock(returnStation, (int) availableCount);

        logger.info("扫码还伞成功: userId={}, umbrellaId={}, returnStationId={}, recordId={}, isCrossRegion={}",
                currentUserId, record.getUmbrellaId(), returnStation.getId(), record.getId(), isCrossRegion);

        ScanReturnResponse response = new ScanReturnResponse();
        response.setRecordId(record.getId());
        response.setUmbrellaId(record.getUmbrellaId());
        response.setUmbrellaCode(umbrella != null ? umbrella.getCode() : null);
        response.setBorrowStationId(borrowStationId);
        response.setBorrowStationName(borrowStation != null ? borrowStation.getName() : null);
        response.setReturnStationId(returnStation.getId());
        response.setReturnStationName(returnStation.getName());
        response.setBorrowTime(record.getBorrowTime());
        response.setReturnTime(record.getReturnTime());
        response.setDeposit(record.getDeposit());
        response.setStatus(record.getStatus());
        response.setAvailableCount((int) availableCount);
        response.setRestockAlert(restockAlert);
        response.setIsCrossRegion(isCrossRegion);
        response.setCrossRegionFee(feeDto);
        response.setCapacityOverflow(false);
        response.setPaymentStatus(record.getPaymentStatus());

        return response;
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
