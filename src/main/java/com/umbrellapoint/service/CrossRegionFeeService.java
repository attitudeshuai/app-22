package com.umbrellapoint.service;

import com.umbrellapoint.config.CrossRegionFeeConfig;
import com.umbrellapoint.dto.fee.CrossRegionFeeDto;
import com.umbrellapoint.dto.fee.CrossRegionFeePaymentRequest;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.CrossRegionFee;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.CrossRegionFeeRepository;
import com.umbrellapoint.repository.StationRepository;
import com.umbrellapoint.repository.UmbrellaRepository;
import com.umbrellapoint.repository.UserCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CrossRegionFeeService {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionFeeService.class);

    private final CrossRegionFeeConfig feeConfig;
    private final CrossRegionFeeRepository feeRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;

    public CrossRegionFeeService(CrossRegionFeeConfig feeConfig,
                                 CrossRegionFeeRepository feeRepository,
                                 BorrowRecordRepository borrowRecordRepository,
                                 StationRepository stationRepository,
                                 UmbrellaRepository umbrellaRepository,
                                 UserCreditRepository userCreditRepository,
                                 AuthService authService) {
        this.feeConfig = feeConfig;
        this.feeRepository = feeRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.userCreditRepository = userCreditRepository;
        this.authService = authService;
    }

    public boolean isCrossRegion(Long borrowStationId, Long returnStationId) {
        if (borrowStationId == null || returnStationId == null) {
            return false;
        }
        return !borrowStationId.equals(returnStationId);
    }

    public BigDecimal calculateDistanceKm(Station borrowStation, Station returnStation) {
        if (borrowStation == null || returnStation == null) {
            return BigDecimal.ZERO;
        }
        if (borrowStation.getLatitude() == null || borrowStation.getLongitude() == null
                || returnStation.getLatitude() == null || returnStation.getLongitude() == null) {
            return BigDecimal.ZERO;
        }
        return calculateHaversineDistance(
                borrowStation.getLatitude().doubleValue(),
                borrowStation.getLongitude().doubleValue(),
                returnStation.getLatitude().doubleValue(),
                returnStation.getLongitude().doubleValue()
        );
    }

    private BigDecimal calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(R * c).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFee(BigDecimal distanceKm) {
        if (!feeConfig.isEnabled()) {
            return BigDecimal.ZERO;
        }
        if (distanceKm == null) {
            distanceKm = BigDecimal.ZERO;
        }
        if (distanceKm.compareTo(feeConfig.getFreeDistanceKm()) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal chargeableDistance = distanceKm.subtract(feeConfig.getFreeDistanceKm());
        BigDecimal baseFee = feeConfig.getBaseFee();
        BigDecimal distanceFee = chargeableDistance.multiply(feeConfig.getPerKmFee())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee = baseFee.add(distanceFee);

        if (totalFee.compareTo(feeConfig.getMinFee()) < 0) {
            totalFee = feeConfig.getMinFee();
        }
        if (totalFee.compareTo(feeConfig.getMaxFee()) > 0) {
            totalFee = feeConfig.getMaxFee();
        }
        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public CrossRegionFee createCrossRegionFee(BorrowRecord record, Station borrowStation, Station returnStation) {
        if (!isCrossRegion(record.getBorrowStationId(), record.getReturnStationId())) {
            return null;
        }
        if (feeRepository.existsByBorrowRecordId(record.getId())) {
            return feeRepository.findByBorrowRecordId(record.getId()).orElse(null);
        }

        BigDecimal distanceKm = calculateDistanceKm(borrowStation, returnStation);
        BigDecimal feeAmount = calculateFee(distanceKm);
        BigDecimal distanceFee = feeAmount.compareTo(BigDecimal.ZERO) > 0
                ? feeAmount.subtract(feeConfig.getBaseFee()).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        CrossRegionFee fee = new CrossRegionFee();
        fee.setBorrowRecordId(record.getId());
        fee.setUserId(record.getUserId());
        fee.setBorrowStationId(record.getBorrowStationId());
        fee.setReturnStationId(record.getReturnStationId());
        fee.setUmbrellaId(record.getUmbrellaId());
        fee.setFeeAmount(feeAmount);
        fee.setBaseFee(feeConfig.getBaseFee());
        fee.setDistanceFee(distanceFee);
        fee.setDistanceKm(distanceKm);
        fee.setStatus(CrossRegionFee.FeeStatus.Pending);
        fee.setSettlementDueDate(LocalDateTime.now().plusDays(feeConfig.getSettlementPeriodDays()));
        fee = feeRepository.save(fee);

        record.setCrossRegionFeeId(fee.getId());
        record.setIsCrossRegion(true);
        record.setPaymentStatus(BorrowRecord.PaymentStatus.Pending);
        borrowRecordRepository.save(record);

        updateUserPendingFees(record.getUserId());

        logger.info("跨区费用创建成功: feeId={}, recordId={}, userId={}, amount={}",
                fee.getId(), record.getId(), record.getUserId(), feeAmount);
        return fee;
    }

    @Transactional
    public CrossRegionFeeDto payFee(CrossRegionFeePaymentRequest request) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        CrossRegionFee fee = feeRepository.findById(request.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("跨区费用", "id", request.getFeeId()));

        if (!fee.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权支付该费用");
        }

        if (fee.getStatus() != CrossRegionFee.FeeStatus.Pending) {
            throw new BusinessException("该费用状态不支持支付，当前状态: " + fee.getStatus());
        }

        if (request.getAmount() != null && request.getAmount().compareTo(fee.getFeeAmount()) != 0) {
            throw new BusinessException("支付金额与应付金额不一致，应付: " + fee.getFeeAmount());
        }

        fee.setStatus(CrossRegionFee.FeeStatus.Paid);
        fee.setPaidAt(LocalDateTime.now());
        fee.setPaymentMethod(request.getPaymentMethod());
        fee.setTransactionId(request.getTransactionId());
        fee = feeRepository.save(fee);

        BorrowRecord record = borrowRecordRepository.findById(fee.getBorrowRecordId()).orElse(null);
        if (record != null) {
            record.setPaymentStatus(BorrowRecord.PaymentStatus.Paid);
            record.setSettledAt(LocalDateTime.now());
            borrowRecordRepository.save(record);
        }

        updateUserPendingFees(fee.getUserId());

        logger.info("跨区费用支付成功: feeId={}, userId={}, amount={}, method={}",
                fee.getId(), fee.getUserId(), fee.getFeeAmount(), request.getPaymentMethod());
        return convertToDto(fee);
    }

    @Transactional
    public CrossRegionFeeDto refundFee(Long feeId, String reason) {
        CrossRegionFee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new ResourceNotFoundException("跨区费用", "id", feeId));

        if (fee.getStatus() != CrossRegionFee.FeeStatus.Paid) {
            throw new BusinessException("只有已支付的费用才能退款，当前状态: " + fee.getStatus());
        }

        LocalDateTime refundDeadline = fee.getPaidAt().plusDays(feeConfig.getRefundValidDays());
        if (LocalDateTime.now().isAfter(refundDeadline)) {
            throw new BusinessException("已超过退款有效期（支付后" + feeConfig.getRefundValidDays() + "天内可退款）");
        }

        fee.setStatus(CrossRegionFee.FeeStatus.Refunded);
        fee.setRefundedAt(LocalDateTime.now());
        fee.setRefundAmount(fee.getFeeAmount());
        fee.setRemark(reason);
        fee = feeRepository.save(fee);

        BorrowRecord record = borrowRecordRepository.findById(fee.getBorrowRecordId()).orElse(null);
        if (record != null) {
            record.setPaymentStatus(BorrowRecord.PaymentStatus.Refunded);
            borrowRecordRepository.save(record);
        }

        logger.info("跨区费用退款成功: feeId={}, userId={}, amount={}",
                fee.getId(), fee.getUserId(), fee.getRefundAmount());
        return convertToDto(fee);
    }

    public Page<CrossRegionFeeDto> getMyFees(int page, int size, CrossRegionFee.FeeStatus status,
                                             String sortBy, String sortDir) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CrossRegionFee> fees;
        if (status != null) {
            fees = feeRepository.findByUserIdAndStatus(currentUserId, status, pageable);
        } else {
            fees = feeRepository.findByUserId(currentUserId, pageable);
        }
        return fees.map(this::convertToDto);
    }

    public Page<CrossRegionFeeDto> getAllFees(int page, int size, Long userId,
                                              CrossRegionFee.FeeStatus status,
                                              String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CrossRegionFee> fees;
        if (status != null) {
            fees = feeRepository.findByStatus(status, pageable);
        } else if (userId != null) {
            fees = feeRepository.findByUserId(userId, pageable);
        } else {
            fees = feeRepository.findAll(pageable);
        }
        return fees.map(this::convertToDto);
    }

    public CrossRegionFeeDto getFeeById(Long id) {
        CrossRegionFee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("跨区费用", "id", id));
        return convertToDto(fee);
    }

    public CrossRegionFeeDto getFeeByBorrowRecordId(Long borrowRecordId) {
        CrossRegionFee fee = feeRepository.findByBorrowRecordId(borrowRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("跨区费用", "borrowRecordId", borrowRecordId));
        return convertToDto(fee);
    }

    private void updateUserPendingFees(Long userId) {
        UserCredit credit = userCreditRepository.findByUserId(userId).orElse(null);
        if (credit == null) {
            credit = new UserCredit();
            credit.setUserId(userId);
            credit.setScore(100);
            credit.setOverdueCount(0);
        }
        BigDecimal pendingFees = feeRepository.sumFeeAmountByUserIdAndStatus(userId, CrossRegionFee.FeeStatus.Pending);
        Integer pendingCount = feeRepository.countByUserIdAndStatus(userId, CrossRegionFee.FeeStatus.Pending);
        credit.setPendingFees(pendingFees != null ? pendingFees : BigDecimal.ZERO);
        credit.setPendingFeeCount(pendingCount != null ? pendingCount : 0);
        userCreditRepository.save(credit);
    }

    public CrossRegionFeeDto convertToDto(CrossRegionFee fee) {
        CrossRegionFeeDto dto = new CrossRegionFeeDto();
        dto.setId(fee.getId());
        dto.setBorrowRecordId(fee.getBorrowRecordId());
        dto.setUserId(fee.getUserId());
        dto.setBorrowStationId(fee.getBorrowStationId());
        dto.setReturnStationId(fee.getReturnStationId());
        dto.setUmbrellaId(fee.getUmbrellaId());
        dto.setFeeAmount(fee.getFeeAmount());
        dto.setBaseFee(fee.getBaseFee());
        dto.setDistanceFee(fee.getDistanceFee());
        dto.setDistanceKm(fee.getDistanceKm());
        dto.setStatus(fee.getStatus());
        dto.setPaidAt(fee.getPaidAt());
        dto.setPaymentMethod(fee.getPaymentMethod());
        dto.setTransactionId(fee.getTransactionId());
        dto.setSettlementDueDate(fee.getSettlementDueDate());
        dto.setRefundedAt(fee.getRefundedAt());
        dto.setRefundAmount(fee.getRefundAmount());
        dto.setRemark(fee.getRemark());
        dto.setCreatedAt(fee.getCreatedAt());
        dto.setUpdatedAt(fee.getUpdatedAt());

        Station borrowStation = stationRepository.findById(fee.getBorrowStationId()).orElse(null);
        if (borrowStation != null) {
            dto.setBorrowStationName(borrowStation.getName());
        }
        Station returnStation = stationRepository.findById(fee.getReturnStationId()).orElse(null);
        if (returnStation != null) {
            dto.setReturnStationName(returnStation.getName());
        }
        Umbrella umbrella = umbrellaRepository.findById(fee.getUmbrellaId()).orElse(null);
        if (umbrella != null) {
            dto.setUmbrellaCode(umbrella.getCode());
        }
        return dto;
    }

    public CrossRegionFeeConfig getFeeConfig() {
        return feeConfig;
    }
}
