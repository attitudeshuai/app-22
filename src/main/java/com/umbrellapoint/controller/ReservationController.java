package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.reservation.ReservationDto;
import com.umbrellapoint.dto.reservation.ReservationRequest;
import com.umbrellapoint.entity.Reservation;
import com.umbrellapoint.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "雨伞预约", description = "雨伞预约相关接口，包括创建预约、取消预约、查询预约等")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "获取预约列表", description = "分页获取所有预约记录，支持按站点和状态筛选")
    public ResponseEntity<ApiResponse<PageResponse<ReservationDto>>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Reservation.ReservationStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<ReservationDto> reservationPage = reservationService.getAllReservations(
                page, size, stationId, status, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(reservationPage)));
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的预约", description = "获取当前登录用户的预约记录列表")
    public ResponseEntity<ApiResponse<PageResponse<ReservationDto>>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<ReservationDto> reservationPage = reservationService.getMyReservations(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(reservationPage)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取预约详情", description = "根据ID获取预约记录详情")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationById(id)));
    }

    @PostMapping
    @Operation(summary = "创建预约", description = "用户发起雨伞预约，选择目标借还点和预计借用时段")
    public ResponseEntity<ApiResponse<ReservationDto>> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationDto reservation = reservationService.createReservation(request);
        String message = reservation.getStatus() == Reservation.ReservationStatus.Active
                ? "预约成功，雨伞已锁定"
                : "预约成功，已加入排队";
        return ResponseEntity.ok(ApiResponse.success(message, reservation));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消预约", description = "用户取消预约，释放锁定的雨伞")
    public ResponseEntity<ApiResponse<ReservationDto>> cancelReservation(@PathVariable Long id) {
        ReservationDto reservation = reservationService.cancelReservation(id);
        return ResponseEntity.ok(ApiResponse.success("预约已取消", reservation));
    }

    @PostMapping("/{id}/pickup")
    @Operation(summary = "确认取伞", description = "用户到达站点后确认取伞，将预约转为正式借还记录")
    public ResponseEntity<ApiResponse<Void>> confirmPickup(@PathVariable Long id) {
        reservationService.confirmPickup(id);
        return ResponseEntity.ok(ApiResponse.success("取伞成功", null));
    }
}
