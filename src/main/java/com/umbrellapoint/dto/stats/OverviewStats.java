package com.umbrellapoint.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStats {
    private long totalUsers;
    private long totalStations;
    private long activeStations;
    private long totalUmbrellas;
    private long availableUmbrellas;
    private long borrowedUmbrellas;
    private long ongoingBorrows;
    private long returnedBorrows;
    private long overdueBorrows;
    private Map<String, Long> umbrellaStatusDistribution;
    private Map<String, Long> borrowStatusDistribution;
}
