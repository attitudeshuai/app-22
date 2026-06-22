package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.credit.CreditChangeLogDto;
import com.umbrellapoint.dto.credit.UserCreditDto;
import com.umbrellapoint.dto.credit.UserCreditRequest;
import com.umbrellapoint.entity.CreditChangeLog;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.service.AuthService;
import com.umbrellapoint.service.UserCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usercredits")
@Tag(name = "信用管理", description = "用户信用管理接口")
public class UserCreditController {

    private final UserCreditService userCreditService;
    private final AuthService authService;

    public UserCreditController(UserCreditService userCreditService, AuthService authService) {
        this.userCreditService = userCreditService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "获取用户信用列表")
    public ResponseEntity<ApiResponse<PageResponse<UserCreditDto>>> getAllCredits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(userCreditService.getAllCredits(page, size, minScore, sortBy, sortDir))));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的信用")
    public ResponseEntity<ApiResponse<UserCreditDto>> getMyCredit() {
        return ResponseEntity.ok(ApiResponse.success(userCreditService.getMyCredit()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信用详情")
    public ResponseEntity<ApiResponse<UserCreditDto>> getCreditById(@PathVariable Long id) {
        UserCreditDto credit = userCreditService.getCreditById(id);
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId != null && !credit.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权查看其他用户的信用信息");
        }
        return ResponseEntity.ok(ApiResponse.success(credit));
    }

    @PostMapping
    @Operation(summary = "创建用户信用")
    public ResponseEntity<ApiResponse<UserCreditDto>> createCredit(@Valid @RequestBody UserCreditRequest request) {
        return ResponseEntity.ok(ApiResponse.success("创建成功", userCreditService.createCredit(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信用")
    public ResponseEntity<ApiResponse<UserCreditDto>> updateCredit(@PathVariable Long id,
                                                                 @Valid @RequestBody UserCreditRequest request) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", userCreditService.updateCredit(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户信用")
    public ResponseEntity<ApiResponse<Void>> deleteCredit(@PathVariable Long id) {
        userCreditService.deleteCredit(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/logs")
    @Operation(summary = "获取信用变更日志")
    public ResponseEntity<ApiResponse<PageResponse<CreditChangeLogDto>>> getCreditChangeLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<CreditChangeLog> logs = userCreditService.getCreditChangeLogs(userId, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(logs.map(this::convertLogToDto))));
    }

    @GetMapping("/mine/logs")
    @Operation(summary = "获取我的信用变更日志")
    public ResponseEntity<ApiResponse<PageResponse<CreditChangeLogDto>>> getMyCreditChangeLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Page<CreditChangeLog> logs = userCreditService.getCreditChangeLogs(
                currentUserId, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(logs.map(this::convertLogToDto))));
    }

    @PostMapping("/appeals/{borrowRecordId}/approve")
    @Operation(summary = "审核通过逾期申诉")
    public ResponseEntity<ApiResponse<Void>> approveAppeal(@PathVariable Long borrowRecordId,
                                                           @RequestParam(required = false) String remark) {
        Long operatorId = authService.getCurrentUserId();
        userCreditService.restoreScoreForAppeal(borrowRecordId, operatorId, remark);
        return ResponseEntity.ok(ApiResponse.success("申诉审核通过，信用分已恢复", null));
    }

    @PostMapping("/appeals/{borrowRecordId}/reject")
    @Operation(summary = "审核驳回逾期申诉")
    public ResponseEntity<ApiResponse<Void>> rejectAppeal(@PathVariable Long borrowRecordId,
                                                          @RequestParam(required = false) String remark) {
        Long operatorId = authService.getCurrentUserId();
        userCreditService.rejectAppeal(borrowRecordId, operatorId, remark);
        return ResponseEntity.ok(ApiResponse.success("申诉已驳回", null));
    }

    private CreditChangeLogDto convertLogToDto(CreditChangeLog log) {
        return new CreditChangeLogDto(
                log.getId(),
                log.getUserId(),
                log.getBorrowRecordId(),
                log.getChangeType(),
                log.getScoreBefore(),
                log.getScoreAfter(),
                log.getScoreChange(),
                log.getOverdueDays(),
                log.getReason(),
                log.getGracePeriodHoursAtTime(),
                log.getPenaltyPerDayAtTime(),
                log.getOperatorId(),
                log.getCreatedAt()
        );
    }
}
