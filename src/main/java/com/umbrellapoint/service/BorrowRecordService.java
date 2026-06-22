package com.umbrellapoint.service;

import com.umbrellapoint.dto.borrow.BorrowRecordDto;
import com.umbrellapoint.dto.borrow.BorrowRecordRequest;
import com.umbrellapoint.dto.borrow.BorrowStatusRequest;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.BorrowRecordRepository;
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
import java.time.LocalDateTime;

@Service
public class BorrowRecordService {

    private static final Logger logger = LoggerFactory.getLogger(BorrowRecordService.class);

    private final BorrowRecordRepository borrowRecordRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;
    private final CreditValidationService creditValidationService;

    public BorrowRecordService(BorrowRecordRepository borrowRecordRepository,
                               UmbrellaRepository umbrellaRepository,
                               UserCreditRepository userCreditRepository,
                               AuthService authService,
                               CreditValidationService creditValidationService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.userCreditRepository = userCreditRepository;
        this.authService = authService;
        this.creditValidationService = creditValidationService;
    }

    public Page<BorrowRecordDto> getAllBorrowRecords(int page, int size, Long umbrellaId,
                                                     BorrowRecord.BorrowStatus status,
                                                     String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BorrowRecord> records;

        if (umbrellaId != null) {
            records = borrowRecordRepository.findByUmbrellaId(umbrellaId, pageable);
        } else if (status != null) {
            records = borrowRecordRepository.findByStatus(status, pageable);
        } else {
            records = borrowRecordRepository.findAll(pageable);
        }
        return records.map(this::convertToDto);
    }

    public Page<BorrowRecordDto> getMyBorrowRecords(int page, int size, String sortBy, String sortDir) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return borrowRecordRepository.findByUserId(currentUserId, pageable)
                .map(this::convertToDto);
    }

    public BorrowRecordDto getBorrowRecordById(Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借还记录", "id", id));
        return convertToDto(record);
    }

    @Transactional
    public BorrowRecordDto createBorrowRecord(BorrowRecordRequest request) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Umbrella umbrella = umbrellaRepository.findById(request.getUmbrellaId())
                .orElseThrow(() -> new ResourceNotFoundException("雨伞", "id", request.getUmbrellaId()));

        if (umbrella.getStatus() != Umbrella.UmbrellaStatus.Available) {
            throw new BusinessException("该雨伞当前不可借");
        }

        creditValidationService.validateBorrowPermission(
                currentUserId, request.getBorrowStationId(), request.getUmbrellaId());

        BorrowRecord record = new BorrowRecord();
        record.setUmbrellaId(request.getUmbrellaId());
        record.setUserId(currentUserId);
        record.setBorrowStationId(request.getBorrowStationId());
        record.setBorrowTime(LocalDateTime.now());
        record.setStatus(BorrowRecord.BorrowStatus.Ongoing);
        record.setDeposit(request.getDeposit() != null ? request.getDeposit() : new BigDecimal("29.90"));
        record = borrowRecordRepository.save(record);

        umbrella.setStatus(Umbrella.UmbrellaStatus.Borrowed);
        umbrellaRepository.save(umbrella);

        logger.info("借伞成功: 用户={}, 雨伞={}", currentUserId, umbrella.getCode());
        return convertToDto(record);
    }

    @Transactional
    public BorrowRecordDto updateBorrowRecord(Long id, BorrowRecordRequest request) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借还记录", "id", id));

        if (request.getUmbrellaId() != null) record.setUmbrellaId(request.getUmbrellaId());
        if (request.getBorrowStationId() != null) record.setBorrowStationId(request.getBorrowStationId());
        if (request.getDeposit() != null) record.setDeposit(request.getDeposit());

        record = borrowRecordRepository.save(record);
        return convertToDto(record);
    }

    @Transactional
    public BorrowRecordDto updateBorrowStatus(Long id, BorrowStatusRequest request) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借还记录", "id", id));

        if (request.getStatus() == BorrowRecord.BorrowStatus.Returned) {
            record.setStatus(BorrowRecord.BorrowStatus.Returned);
            record.setReturnTime(LocalDateTime.now());
            if (request.getReturnStationId() != null) {
                record.setReturnStationId(request.getReturnStationId());
            }

            Umbrella umbrella = umbrellaRepository.findById(record.getUmbrellaId()).orElse(null);
            if (umbrella != null) {
                umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
                if (request.getReturnStationId() != null) {
                    umbrella.setStationId(request.getReturnStationId());
                }
                umbrellaRepository.save(umbrella);
            }
            logger.info("还伞成功: 记录={}", id);
        } else if (request.getStatus() == BorrowRecord.BorrowStatus.Overdue) {
            record.setStatus(BorrowRecord.BorrowStatus.Overdue);

            UserCredit credit = userCreditRepository.findByUserId(record.getUserId()).orElse(null);
            if (credit != null) {
                credit.setScore(Math.max(0, credit.getScore() - 5));
                credit.setOverdueCount(credit.getOverdueCount() + 1);
                userCreditRepository.save(credit);
            }
            logger.info("借还记录标记为逾期: {}", id);
        } else {
            record.setStatus(request.getStatus());
        }

        record = borrowRecordRepository.save(record);
        return convertToDto(record);
    }

    @Transactional
    public void deleteBorrowRecord(Long id) {
        if (!borrowRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("借还记录", "id", id);
        }
        borrowRecordRepository.deleteById(id);
        logger.info("借还记录删除成功: {}", id);
    }

    private BorrowRecordDto convertToDto(BorrowRecord record) {
        return new BorrowRecordDto(
                record.getId(),
                record.getUmbrellaId(),
                record.getUserId(),
                record.getBorrowStationId(),
                record.getReturnStationId(),
                record.getBorrowTime(),
                record.getReturnTime(),
                record.getStatus(),
                record.getDeposit(),
                record.getCrossRegionFeeId(),
                record.getIsCrossRegion(),
                record.getPaymentStatus(),
                record.getSettledAt(),
                record.getCreatedAt()
        );
    }
}
