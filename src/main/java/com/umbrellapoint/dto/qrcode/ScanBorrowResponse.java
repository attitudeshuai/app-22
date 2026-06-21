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
public class ScanBorrowResponse {
    private Long recordId;
    private Long umbrellaId;
    private String umbrellaCode;
    private Long stationId;
    private String stationName;
    private LocalDateTime borrowTime;
    private BigDecimal deposit;
    private BorrowRecord.BorrowStatus status;
    private Integer availableCount;
    private boolean restockAlert;
}
