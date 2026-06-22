package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.credit.UserCreditDto;
import com.umbrellapoint.dto.credit.UserCreditRequest;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.service.AuthService;
import com.umbrellapoint.service.UserCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
}
