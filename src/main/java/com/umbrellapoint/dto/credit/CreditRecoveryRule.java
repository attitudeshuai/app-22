package com.umbrellapoint.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditRecoveryRule {
    private Integer dailyRecoveryScore;
    private Integer maxRecoveryScore;
    private String description;
}
