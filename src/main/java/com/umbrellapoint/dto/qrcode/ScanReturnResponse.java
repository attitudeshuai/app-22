package com.umbrellapoint.dto.qrcode;

import com.umbrellapoint.dto.fee.CrossRegionFeeDto;
import com.umbrellapoint.dto.station.NearbyStationDto;
import com.umbrellapoint.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private Boolean isCrossRegion;
    private CrossRegionFeeDto crossRegionFee;
    private Boolean capacityOverflow;
    private List<NearbyStationDto> nearbyStations;
    private BorrowRecord.PaymentStatus paymentStatus;
}
