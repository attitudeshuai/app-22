package com.umbrellapoint.service;

import com.umbrellapoint.dto.credit.UserCreditDto;
import com.umbrellapoint.dto.credit.UserCreditRequest;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.CreditChangeLog;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.CreditChangeLogRepository;
import com.umbrellapoint.repository.UserCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class UserCreditService {

    private static final Logger logger = LoggerFactory.getLogger(UserCreditService.class);

    private final UserCreditRepository userCreditRepository;
    private final CreditChangeLogRepository creditChangeLogRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final CreditConfigService creditConfigService;
    private final AuthService authService;

    public UserCreditService(UserCreditRepository userCreditRepository,
                             CreditChangeLogRepository creditChangeLogRepository,
                             BorrowRecordRepository borrowRecordRepository,
                             CreditConfigService creditConfigService,
                             AuthService authService) {
        this.userCreditRepository = userCreditRepository;
        this.creditChangeLogRepository = creditChangeLogRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.creditConfigService = creditConfigService;
        this.authService = authService;
    }

    public Page<UserCreditDto> getAllCredits(int page, int size, Integer minScore, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserCredit> credits;

        if (minScore != null) {
            credits = userCreditRepository.findByScoreLessThan(minScore, pageable);
        } else {
            credits = userCreditRepository.findAll(pageable);
        }
        return credits.map(this::convertToDto);
    }

    public UserCreditDto getMyCredit() {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        UserCredit credit = userCreditRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户信用", "userId", currentUserId));
        return convertToDto(credit);
    }

    public UserCreditDto getCreditById(Long id) {
        UserCredit credit = userCreditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户信用", "id", id));
        return convertToDto(credit);
    }

    @Transactional
    public UserCreditDto createCredit(UserCreditRequest request) {
        if (userCreditRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException("该用户信用记录已存在");
        }
        UserCredit credit = new UserCredit();
        credit.setUserId(request.getUserId());
        credit.setScore(request.getScore() != null ? request.getScore() : 100);
        credit.setOverdueCount(request.getOverdueCount() != null ? request.getOverdueCount() : 0);
        credit = userCreditRepository.save(credit);
        logger.info("用户信用创建成功: userId={}", credit.getUserId());
        return convertToDto(credit);
    }

    @Transactional
    public UserCreditDto updateCredit(Long id, UserCreditRequest request) {
        UserCredit credit = userCreditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户信用", "id", id));

        if (request.getScore() != null) credit.setScore(request.getScore());
        if (request.getOverdueCount() != null) credit.setOverdueCount(request.getOverdueCount());

        credit = userCreditRepository.save(credit);
        logger.info("用户信用更新成功: id={}", id);
        return convertToDto(credit);
    }

    @Transactional
    public void deleteCredit(Long id) {
        if (!userCreditRepository.existsById(id)) {
            throw new ResourceNotFoundException("用户信用", "id", id);
        }
        userCreditRepository.deleteById(id);
        logger.info("用户信用删除成功: id={}", id);
    }

    public boolean hasPenaltyToday(Long userId, Long borrowRecordId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return creditChangeLogRepository.existsPenaltyForRecordOnDate(
                userId, borrowRecordId, CreditChangeLog.ChangeType.OVERDUE_PENALTY, LocalDateTime.now());
    }

    @Transactional
    public int applyOverduePenalty(BorrowRecord record) {
        Long userId = record.getUserId();
        Long borrowRecordId = record.getId();

        if (hasPenaltyToday(userId, borrowRecordId)) {
            logger.debug("今日已对该记录执行过逾期扣减，跳过: borrowRecordId={}", borrowRecordId);
            return 0;
        }

        UserCredit credit = userCreditRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户信用", "userId", userId));

        int gracePeriodHours = creditConfigService.getGracePeriodHours();
        int penaltyPerDay = creditConfigService.getOverduePenaltyPerDay();
        int maxPenalty = creditConfigService.getMaxOverduePenalty();

        LocalDateTime deadline = record.getBorrowTime().plusHours(gracePeriodHours);
        long hoursOverdue = java.time.Duration.between(deadline, LocalDateTime.now()).toHours();
        if (hoursOverdue <= 0) {
            logger.debug("还在宽限期内，不扣减: borrowRecordId={}", borrowRecordId);
            return 0;
        }

        int overdueDays = (int) Math.ceil(hoursOverdue / 24.0);
        int totalPenalty = Math.min(overdueDays * penaltyPerDay, maxPenalty);

        int existingPenalty = getTotalPenaltyForRecord(borrowRecordId);
        int additionalPenalty = Math.max(0, totalPenalty - existingPenalty);

        if (additionalPenalty <= 0) {
            logger.debug("已达最大扣减额度，跳过: borrowRecordId={}, totalPenalty={}", borrowRecordId, totalPenalty);
            return 0;
        }

        int scoreBefore = credit.getScore();
        int scoreAfter = Math.max(0, scoreBefore - additionalPenalty);
        int actualDeduction = scoreBefore - scoreAfter;

        if (actualDeduction <= 0) {
            logger.debug("信用分已为0，无法再扣减: userId={}", userId);
            return 0;
        }

        credit.setScore(scoreAfter);
        credit.setOverdueCount(credit.getOverdueCount() + 1);
        userCreditRepository.save(credit);

        CreditChangeLog log = new CreditChangeLog();
        log.setUserId(userId);
        log.setBorrowRecordId(borrowRecordId);
        log.setChangeType(CreditChangeLog.ChangeType.OVERDUE_PENALTY);
        log.setScoreBefore(scoreBefore);
        log.setScoreAfter(scoreAfter);
        log.setScoreChange(-actualDeduction);
        log.setOverdueDays(overdueDays);
        log.setReason("逾期第" + overdueDays + "天，扣减信用分" + actualDeduction + "分");
        log.setGracePeriodHoursAtTime(gracePeriodHours);
        log.setPenaltyPerDayAtTime(penaltyPerDay);
        creditChangeLogRepository.save(log);

        record.setLastPenaltyAt(LocalDateTime.now());
        record.setTotalOverdueDays(overdueDays);
        borrowRecordRepository.save(record);

        logger.info("逾期扣减信用分: userId={}, borrowRecordId={}, overdueDays={}, deduction={}, scoreBefore={}, scoreAfter={}",
                userId, borrowRecordId, overdueDays, actualDeduction, scoreBefore, scoreAfter);

        return actualDeduction;
    }

    public int getTotalPenaltyForRecord(Long borrowRecordId) {
        return creditChangeLogRepository.findByBorrowRecordId(borrowRecordId).stream()
                .filter(log -> log.getChangeType() == CreditChangeLog.ChangeType.OVERDUE_PENALTY)
                .mapToInt(log -> -log.getScoreChange())
                .sum();
    }

    public Page<CreditChangeLog> getCreditChangeLogs(Long userId, int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        if (userId != null) {
            return creditChangeLogRepository.findByUserId(userId, pageable);
        }
        return creditChangeLogRepository.findAll(pageable);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreScoreForAppeal(Long borrowRecordId, Long operatorId, String remark) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("借阅记录", "id", borrowRecordId));

        if (record.getAppealStatus() != BorrowRecord.AppealStatus.Pending) {
            throw new BusinessException("该记录申诉状态不是待审核，无法处理");
        }

        record.setAppealStatus(BorrowRecord.AppealStatus.Approved);
        record.setAppealReviewTime(LocalDateTime.now());
        record.setAppealReviewerId(operatorId);
        record.setAppealReviewRemark(remark);
        borrowRecordRepository.save(record);

        int totalRestored = 0;
        UserCredit credit = userCreditRepository.findByUserId(record.getUserId()).orElse(null);
        if (credit != null) {
            int scoreBefore = credit.getScore();

            totalRestored = getTotalPenaltyForRecord(borrowRecordId);
            if (totalRestored > 0) {
                int newScore = Math.min(100, scoreBefore + totalRestored);
                int actualRestore = newScore - scoreBefore;
                credit.setScore(newScore);
                userCreditRepository.save(credit);

                CreditChangeLog log = new CreditChangeLog();
                log.setUserId(record.getUserId());
                log.setBorrowRecordId(borrowRecordId);
                log.setChangeType(CreditChangeLog.ChangeType.APPEAL_RESTORE);
                log.setScoreBefore(scoreBefore);
                log.setScoreAfter(newScore);
                log.setScoreChange(actualRestore);
                log.setReason("申诉通过，恢复信用分" + actualRestore + "分。" + (remark != null ? "备注：" + remark : ""));
                log.setOperatorId(operatorId);
                creditChangeLogRepository.save(log);

                logger.info("申诉通过恢复信用分: userId={}, borrowRecordId={}, restored={}, scoreBefore={}, scoreAfter={}",
                        record.getUserId(), borrowRecordId, actualRestore, scoreBefore, newScore);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectAppeal(Long borrowRecordId, Long operatorId, String remark) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("借阅记录", "id", borrowRecordId));

        if (record.getAppealStatus() != BorrowRecord.AppealStatus.Pending) {
            throw new BusinessException("该记录申诉状态不是待审核，无法处理");
        }

        record.setAppealStatus(BorrowRecord.AppealStatus.Rejected);
        record.setAppealReviewTime(LocalDateTime.now());
        record.setAppealReviewerId(operatorId);
        record.setAppealReviewRemark(remark);
        borrowRecordRepository.save(record);

        logger.info("申诉驳回: borrowRecordId={}, operatorId={}, remark={}", borrowRecordId, operatorId, remark);
    }

    private UserCreditDto convertToDto(UserCredit credit) {
        return new UserCreditDto(
                credit.getId(),
                credit.getUserId(),
                credit.getScore(),
                credit.getOverdueCount(),
                credit.getPendingFees(),
                credit.getPendingFeeCount(),
                credit.getUpdatedAt()
        );
    }
}
