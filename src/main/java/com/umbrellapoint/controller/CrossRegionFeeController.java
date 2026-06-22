package com.umbrellapoint.controller;

import com.umbrellapoint.config.CrossRegionFeeConfig;
import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.fee.CrossRegionFeeDto;
import com.umbrellapoint.dto.fee.CrossRegionFeePaymentRequest;
import com.umbrellapoint.entity.CrossRegionFee;
import com.umbrellapoint.service.CrossRegionFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cross-region-fees")
@Tag(name = "跨区费用管理", description = "跨区归还雨伞的费用管理接口")
public class CrossRegionFeeController {

    private final CrossRegionFeeService crossRegionFeeService;

    public CrossRegionFeeController(CrossRegionFeeService crossRegionFeeService) {
        this.crossRegionFeeService = crossRegionFeeService;
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的跨区费用列表", description = "获取当前登录用户的所有跨区费用记录，可按状态筛选")
    public ResponseEntity<ApiResponse<PageResponse<CrossRegionFeeDto>>> getMyFees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CrossRegionFee.FeeStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(crossRegionFeeService.getMyFees(page, size, status, sortBy, sortDir))));
    }

    @GetMapping
    @Operation(summary = "获取所有跨区费用列表", description = "管理员获取所有跨区费用记录")
    public ResponseEntity<ApiResponse<PageResponse<CrossRegionFeeDto>>> getAllFees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) CrossRegionFee.FeeStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(crossRegionFeeService.getAllFees(page, size, userId, status, sortBy, sortDir))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取跨区费用详情", description = "根据ID获取跨区费用详情")
    public ResponseEntity<ApiResponse<CrossRegionFeeDto>> getFeeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(crossRegionFeeService.getFeeById(id)));
    }

    @GetMapping("/by-record/{borrowRecordId}")
    @Operation(summary = "根据借还记录ID获取跨区费用", description = "根据借还记录ID查询关联的跨区费用")
    public ResponseEntity<ApiResponse<CrossRegionFeeDto>> getFeeByBorrowRecordId(
            @PathVariable Long borrowRecordId) {
        return ResponseEntity.ok(ApiResponse.success(crossRegionFeeService.getFeeByBorrowRecordId(borrowRecordId)));
    }

    @PostMapping("/pay")
    @Operation(summary = "支付跨区费用", description = "用户支付待缴的跨区费用，支付完成后借还记录正式结清")
    public ResponseEntity<ApiResponse<CrossRegionFeeDto>> payFee(
            @Valid @RequestBody CrossRegionFeePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("支付成功", crossRegionFeeService.payFee(request)));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "跨区费用退款", description = "管理员对已支付的跨区费用执行退款操作，需在退款有效期内")
    public ResponseEntity<ApiResponse<CrossRegionFeeDto>> refundFee(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("退款成功", crossRegionFeeService.refundFee(id, reason)));
    }

    @GetMapping("/config")
    @Operation(summary = "获取跨区费用配置", description = "获取当前跨区费用的计算规则、结算周期、退款规则等后台配置")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeeConfig() {
        CrossRegionFeeConfig config = crossRegionFeeService.getFeeConfig();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "enabled", config.isEnabled(),
                "baseFee", config.getBaseFee(),
                "perKmFee", config.getPerKmFee(),
                "minFee", config.getMinFee(),
                "maxFee", config.getMaxFee(),
                "freeDistanceKm", config.getFreeDistanceKm(),
                "settlementPeriodDays", config.getSettlementPeriodDays(),
                "refundValidDays", config.getRefundValidDays(),
                "nearbyStationRadiusKm", config.getNearbyStationRadiusKm(),
                "nearbyStationLimit", config.getNearbyStationLimit()
        )));
    }
}
