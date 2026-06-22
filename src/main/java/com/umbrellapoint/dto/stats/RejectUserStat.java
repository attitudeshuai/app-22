package com.umbrellapoint.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectUserStat {
    private Long userAnonymizedId;
    private Long rejectCount;
}
