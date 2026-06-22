package com.umbrellapoint.dto.fee;

import com.umbrellapoint.entity.CrossRegionFee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrossRegionFeeDto {
    private Long id;
    private Long borrowRecordId;
    private Long userId;
    private Long borrowStationId;
    private String borrowStationName;
    private Long returnStationId;
    private String returnStationName;
    private Long umbrellaId;
    private String umbrellaCode;
    private BigDecimal feeAmount;
    private BigDecimal baseFee;
    private BigDecimal distanceFee;
    private BigDecimal distanceKm;
    private CrossRegionFee.FeeStatus status;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime settlementDueDate;
    private LocalDateTime refundedAt;
    private BigDecimal refundAmount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
