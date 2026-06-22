package com.umbrellapoint.service;

import com.umbrellapoint.dto.credit.UserCreditDto;
import com.umbrellapoint.dto.credit.UserCreditRequest;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.UserCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCreditService {

    private static final Logger logger = LoggerFactory.getLogger(UserCreditService.class);

    private final UserCreditRepository userCreditRepository;
    private final AuthService authService;

    public UserCreditService(UserCreditRepository userCreditRepository, AuthService authService) {
        this.userCreditRepository = userCreditRepository;
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
