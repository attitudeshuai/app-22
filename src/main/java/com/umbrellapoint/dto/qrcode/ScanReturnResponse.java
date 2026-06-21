package com.umbrellapoint.dto.qrcode;

import com.umbrellapoint.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanReturnResponse {
    private Long recordId;
    private Long umbrellaId;
    private String umbrellaCode;
    private Long borrowStationId;
    private String borrowStationName;
    private Long returnStationId;
    private String returnStationName;
    private LocalDateTime borrowTime;
    private LocalDateTime returnTime;
    private BigDecimal deposit;
    private BorrowRecord.BorrowStatus status;
    private Integer availableCount;
    private boolean restockAlert;
}
