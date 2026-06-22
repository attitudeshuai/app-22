package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.borrow.BorrowRecordDto;
import com.umbrellapoint.dto.borrow.BorrowRecordRequest;
import com.umbrellapoint.dto.borrow.BorrowStatusRequest;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.service.AuthService;
import com.umbrellapoint.service.BorrowRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrowrecords")
@Tag(name = "借还管理", description = "借还记录CRUD和状态管理接口")
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;
    private final AuthService authService;

    public BorrowRecordController(BorrowRecordService borrowRecordService, AuthService authService) {
        this.borrowRecordService = borrowRecordService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "获取借还记录列表")
    public ResponseEntity<ApiResponse<PageResponse<BorrowRecordDto>>> getAllBorrowRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long umbrellaId,
            @RequestParam(required = false) BorrowRecord.BorrowStatus status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(borrowRecordService.getAllBorrowRecords(
                        page, size, umbrellaId, status, sortBy, sortDir))));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的借还记录")
    public ResponseEntity<ApiResponse<PageResponse<BorrowRecordDto>>> getMyBorrowRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(borrowRecordService.getMyBorrowRecords(page, size, sortBy, sortDir))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取借还记录详情")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> getBorrowRecordById(@PathVariable Long id) {
        BorrowRecordDto record = borrowRecordService.getBorrowRecordById(id);
        Long currentUserId = authService.getCurrentUserId();
        if (currentUserId != null && !record.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权查看其他用户的借还记录");
        }
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping
    @Operation(summary = "创建借伞")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> createBorrowRecord(
            @Valid @RequestBody BorrowRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("借伞成功", borrowRecordService.createBorrowRecord(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新借还记录")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> updateBorrowRecord(
            @PathVariable Long id,
            @Valid @RequestBody BorrowRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("更新成功",
                borrowRecordService.updateBorrowRecord(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改借还状态（还伞/逾期等）")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> updateBorrowStatus(
            @PathVariable Long id,
            @Valid @RequestBody BorrowStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("状态更新成功",
                borrowRecordService.updateBorrowStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除借还记录")
    public ResponseEntity<ApiResponse<Void>> deleteBorrowRecord(@PathVariable Long id) {
        borrowRecordService.deleteBorrowRecord(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/{id}/appeal")
    @Operation(summary = "提交逾期申诉")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> submitAppeal(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                "申诉提交成功",
                borrowRecordService.submitAppeal(id, reason)));
    }
}
