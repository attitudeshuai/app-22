package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.qrcode.ScanBorrowRequest;
import com.umbrellapoint.dto.qrcode.ScanBorrowResponse;
import com.umbrellapoint.dto.qrcode.ScanReturnRequest;
import com.umbrellapoint.dto.qrcode.ScanReturnResponse;
import com.umbrellapoint.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qrcode")
@Tag(name = "扫码借还", description = "用户扫描借还点二维码进行借伞/还伞的接口")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/borrow")
    @Operation(summary = "扫码借伞", description = "用户扫描借还点二维码借伞，需校验站点状态、库存余量、用户信用分")
    public ResponseEntity<ApiResponse<ScanBorrowResponse>> scanBorrow(
            @Valid @RequestBody ScanBorrowRequest request) {
        return ResponseEntity.ok(ApiResponse.success("借伞成功", qrCodeService.scanBorrow(request)));
    }

    @PostMapping("/return")
    @Operation(summary = "扫码还伞", description = "用户扫描借还点二维码还伞，需校验站点状态和容量")
    public ResponseEntity<ApiResponse<ScanReturnResponse>> scanReturn(
            @Valid @RequestBody ScanReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("还伞成功", qrCodeService.scanReturn(request)));
    }
}
