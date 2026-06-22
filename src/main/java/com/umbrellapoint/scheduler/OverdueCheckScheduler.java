package com.umbrellapoint.scheduler;

import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.UserCreditRepository;
import com.umbrellapoint.service.CreditConfigService;
import com.umbrellapoint.service.OperationLogService;
import com.umbrellapoint.service.UserCreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OverdueCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OverdueCheckScheduler.class);

    private final BorrowRecordRepository borrowRecordRepository;
    private final UserCreditRepository userCreditRepository;
    private final UserCreditService userCreditService;
    private final CreditConfigService creditConfigService;
    private final OperationLogService operationLogService;

    public OverdueCheckScheduler(BorrowRecordRepository borrowRecordRepository,
                                 UserCreditRepository userCreditRepository,
                                 UserCreditService userCreditService,
                                 CreditConfigService creditConfigService,
                                 OperationLogService operationLogService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
        this.userCreditService = userCreditService;
        this.creditConfigService = creditConfigService;
        this.operationLogService = operationLogService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkOverdueBorrows() {
        logger.info("开始检查逾期借还记录...");

        int gracePeriodHours = creditConfigService.getGracePeriodHours();
        LocalDateTime overdueThreshold = LocalDateTime.now().minusHours(gracePeriodHours);

        List<BorrowRecord> overdueRecords = borrowRecordRepository
                .findByStatusInAndAppealStatusNotAndBorrowTimeBefore(
                        java.util.Arrays.asList(
                                BorrowRecord.BorrowStatus.Ongoing,
                                BorrowRecord.BorrowStatus.Overdue
                        ),
                        BorrowRecord.AppealStatus.Pending,
                        overdueThreshold);

        logger.info("找到 {} 条逾期或即将逾期的记录（已排除申诉待审核）", overdueRecords.size());

        int newOverdueCount = 0;
        int penaltyCount = 0;
        int totalDeduction = 0;
        int skippedPenaltyCount = 0;

        for (BorrowRecord record : overdueRecords) {
            if (record.getStatus() != BorrowRecord.BorrowStatus.Overdue) {
                record.setStatus(BorrowRecord.BorrowStatus.Overdue);
                borrowRecordRepository.save(record);
                newOverdueCount++;
                logger.info("新记录标记为逾期: borrowRecordId={}, userId={}", record.getId(), record.getUserId());
            }

            if (userCreditService.hasPenaltyToday(record.getUserId(), record.getId())) {
                skippedPenaltyCount++;
                logger.debug("今日已扣减，跳过: borrowRecordId={}", record.getId());
                continue;
            }

            int deduction = userCreditService.applyOverduePenalty(record);
            if (deduction > 0) {
                penaltyCount++;
                totalDeduction += deduction;

                if (creditConfigService.isOverdueNotifyEnabled()) {
                    sendOverdueNotification(record, deduction);
                }
            }
        }

        logger.info("逾期检查完成，新标记逾期 {} 条，今日执行扣减 {} 条，跳过重复扣减 {} 条，累计扣减 {} 分",
                newOverdueCount, penaltyCount, skippedPenaltyCount, totalDeduction);
    }

    private void sendOverdueNotification(BorrowRecord record, int deduction) {
        try {
            operationLogService.logUmbrellaBorrowReject(
                    record.getUserId(),
                    record.getBorrowStationId(),
                    record.getUmbrellaId(),
                    "逾期通知：您的借伞记录（ID：" + record.getId() + "）已逾期，扣减信用分" + deduction + "分，请尽快归还。",
                    "OVERDUE_NOTIFY"
            );
            logger.debug("逾期通知已发送: userId={}, borrowRecordId={}", record.getUserId(), record.getId());
        } catch (Exception e) {
            logger.warn("发送逾期通知失败: borrowRecordId={}, error={}", record.getId(), e.getMessage());
        }
    }
}
