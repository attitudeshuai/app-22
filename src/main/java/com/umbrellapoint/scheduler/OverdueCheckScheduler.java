package com.umbrellapoint.scheduler;

import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.UserCreditRepository;
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

    public OverdueCheckScheduler(BorrowRecordRepository borrowRecordRepository,
                                 UserCreditRepository userCreditRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkOverdueBorrows() {
        logger.info("开始检查逾期借还记录...");
        LocalDateTime overdueThreshold = LocalDateTime.now().minusHours(24);
        List<BorrowRecord> ongoingRecords = borrowRecordRepository
                .findByStatusAndBorrowTimeBefore(BorrowRecord.BorrowStatus.Ongoing, overdueThreshold);

        int count = 0;
        for (BorrowRecord record : ongoingRecords) {
            record.setStatus(BorrowRecord.BorrowStatus.Overdue);
            borrowRecordRepository.save(record);

            UserCredit credit = userCreditRepository.findByUserId(record.getUserId()).orElse(null);
            if (credit != null) {
                credit.setScore(Math.max(0, credit.getScore() - 5));
                credit.setOverdueCount(credit.getOverdueCount() + 1);
                userCreditRepository.save(credit);
            }
            count++;
        }
        logger.info("逾期检查完成，共处理 {} 条逾期记录", count);
    }
}
