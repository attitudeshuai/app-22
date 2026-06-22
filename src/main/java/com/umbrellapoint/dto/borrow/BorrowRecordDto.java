package com.umbrellapoint.dto.borrow;

import com.umbrellapoint.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordDto {
    private Long id;
    private Long umbrellaId;
    private Long userId;
    private Long borrowStationId;
    private Long returnStationId;
    private LocalDateTime borrowTime;
    private LocalDateTime returnTime;
    private BorrowRecord.BorrowStatus status;
    private BigDecimal deposit;
    private Long crossRegionFeeId;
    private Boolean isCrossRegion;
    private BorrowRecord.PaymentStatus paymentStatus;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
}
