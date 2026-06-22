package com.umbrellapoint.dto.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonStatsDto {
    private Boolean hasData;
    private String remark;
    private BigDecimal totalBorrowReturnChange;
    private BigDecimal totalBorrowReturnChangeRate;
    private BigDecimal overdueCountChange;
    private BigDecimal overdueCountChangeRate;
    private BigDecimal overdueRateChange;
    private BigDecimal avgDurationChange;
    private BigDecimal avgDurationChangeRate;
    private BigDecimal availableUmbrellasChange;
    private BigDecimal availableUmbrellasChangeRate;
    private String comparisonType;
    private String comparisonPeriod;
}
