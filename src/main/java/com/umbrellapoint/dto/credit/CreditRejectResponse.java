package com.umbrellapoint.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditRejectResponse {
    private Integer currentScore;
    private Integer minRequiredScore;
    private Integer scoreGap;
    private Integer currentBorrowLimit;
    private Integer ongoingBorrowCount;
    private CreditRecoveryRule recoveryRule;
}
