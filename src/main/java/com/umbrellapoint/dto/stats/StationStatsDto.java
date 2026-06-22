package com.umbrellapoint.dto.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationStatsDto {
    private Long stationId;
    private String stationName;
    private String stationAddress;
    private Integer totalBorrowReturn;
    private Integer borrowCount;
    private Integer returnCount;
    private Integer overdueCount;
    private BigDecimal overdueRate;
    private BigDecimal avgBorrowDurationMinutes;
    private Integer availableUmbrellas;
    private Integer totalUmbrellas;
    private Integer capacity;
}
