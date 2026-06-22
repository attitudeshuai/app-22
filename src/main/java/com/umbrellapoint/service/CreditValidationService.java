package com.umbrellapoint.service;

import com.umbrellapoint.dto.credit.CreditRecoveryRule;
import com.umbrellapoint.dto.credit.CreditRejectResponse;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.UserCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreditValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CreditValidationService.class);

    private final UserCreditRepository userCreditRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final OperationLogService operationLogService;

    @Value("${credit.min-score:60}")
    private int minCreditScore;

    @Value("${credit.default-borrow-limit:1}")
    private int defaultBorrowLimit;

    @Value("${credit.tier1-threshold:70}")
    private int tier1Threshold;

    @Value("${credit.tier1-borrow-limit:2}")
    private int tier1BorrowLimit;

    @Value("${credit.tier2-threshold:85}")
    private int tier2Threshold;

    @Value("${credit.tier2-borrow-limit:3}")
    private int tier2BorrowLimit;

    @Value("${credit.tier3-threshold:95}")
    private int tier3Threshold;

    @Value("${credit.tier3-borrow-limit:5}")
    private int tier3BorrowLimit;

    @Value("${credit.score-recovery-daily:1}")
    private int dailyRecoveryScore;

    @Value("${credit.score-recovery-max:100}")
    private int maxRecoveryScore;

    public CreditValidationService(UserCreditRepository userCreditRepository,
                                   BorrowRecordRepository borrowRecordRepository,
                                   OperationLogService operationLogService) {
        this.userCreditRepository = userCreditRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.operationLogService = operationLogService;
    }

    public int calculateBorrowLimit(int creditScore) {
        if (creditScore >= tier3Threshold) {
            return tier3BorrowLimit;
        } else if (creditScore >= tier2Threshold) {
            return tier2BorrowLimit;
        } else if (creditScore >= tier1Threshold) {
            return tier1BorrowLimit;
        }
        return defaultBorrowLimit;
    }

    public int getMinCreditScore() {
        return minCreditScore;
    }

    public CreditRecoveryRule getRecoveryRule() {
        String description = String.format(
                "按时归还雨伞可保持信用分，逾期每次扣%d分。信用分每日自动恢复%d分，最高恢复至%d分。",
                5, dailyRecoveryScore, maxRecoveryScore);
        return new CreditRecoveryRule(dailyRecoveryScore, maxRecoveryScore, description);
    }

    public void validateBorrowPermission(Long userId, Long stationId, Long umbrellaId) {
        UserCredit credit = userCreditRepository.findByUserId(userId).orElse(null);
        int currentScore = credit != null ? credit.getScore() : 0;

        if (currentScore < minCreditScore) {
            int scoreGap = minCreditScore - currentScore;
            int borrowLimit = calculateBorrowLimit(currentScore);
            long ongoingCount = borrowRecordRepository.countByUserIdAndStatus(
                    userId, BorrowRecord.BorrowStatus.Ongoing);

            CreditRejectResponse rejectResponse = new CreditRejectResponse(
                    currentScore,
                    minCreditScore,
                    scoreGap,
                    borrowLimit,
                    (int) ongoingCount,
                    getRecoveryRule()
            );

            String rejectReason = String.format(
                    "信用分不足：当前%d分，最低需要%d分，还差%d分",
                    currentScore, minCreditScore, scoreGap);

            operationLogService.logUmbrellaBorrowReject(
                    userId, stationId, umbrellaId, "借伞被拒-信用分不足", rejectReason);

            logger.warn("借伞被拒: 信用分不足, userId={}, score={}, minScore={}", userId, currentScore, minCreditScore);
            throw new BusinessException(403, rejectReason, rejectResponse);
        }

        int borrowLimit = calculateBorrowLimit(currentScore);
        long ongoingCount = borrowRecordRepository.countByUserIdAndStatus(
                userId, BorrowRecord.BorrowStatus.Ongoing);

        if (ongoingCount >= borrowLimit) {
            CreditRejectResponse rejectResponse = new CreditRejectResponse(
                    currentScore,
                    minCreditScore,
                    0,
                    borrowLimit,
                    (int) ongoingCount,
                    getRecoveryRule()
            );

            String rejectReason = String.format(
                    "已达到借伞数量上限：当前持有%d把，信用分%d分对应的上限为%d把",
                    ongoingCount, currentScore, borrowLimit);

            operationLogService.logUmbrellaBorrowReject(
                    userId, stationId, umbrellaId, "借伞被拒-超出数量上限", rejectReason);

            logger.warn("借伞被拒: 超出借伞上限, userId={}, ongoing={}, limit={}", userId, ongoingCount, borrowLimit);
            throw new BusinessException(403, rejectReason, rejectResponse);
        }
    }
}
