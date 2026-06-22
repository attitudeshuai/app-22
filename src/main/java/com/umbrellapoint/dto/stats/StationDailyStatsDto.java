package com.umbrellapoint.dto.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationDailyStatsDto {
    private Long id;
    private LocalDate statDate;
    private Long stationId;
    private String stationName;
    private Integer borrowCount;
    private Integer returnCount;
    private Integer totalBorrowReturn;
    private Integer overdueCount;
    private BigDecimal overdueRate;
    private BigDecimal avgBorrowDurationMinutes;
    private Integer availableUmbrellas;
    private Integer totalUmbrellas;
    private Integer newUsers;
    private Integer creditDeductionCount;
    private Integer crossRegionCount;
}
