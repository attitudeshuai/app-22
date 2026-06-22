package com.umbrellapoint.dto.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportDto {
    private LocalDate reportDate;
    private Integer totalStations;
    private Integer totalBorrowReturn;
    private Integer totalBorrows;
    private Integer totalReturns;
    private Integer totalOverdue;
    private BigDecimal overallOverdueRate;
    private BigDecimal overallAvgDurationMinutes;
    private Integer totalAvailableUmbrellas;
    private Integer totalUmbrellas;
    private ComparisonStatsDto dayOverDay;
    private ComparisonStatsDto weekOverWeek;
    private List<StationDailyStatsDto> stationStats;
}
