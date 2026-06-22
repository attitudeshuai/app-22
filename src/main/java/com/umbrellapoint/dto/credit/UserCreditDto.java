package com.umbrellapoint.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreditDto {
    private Long id;
    private Long userId;
    private Integer score;
    private Integer overdueCount;
    private BigDecimal pendingFees;
    private Integer pendingFeeCount;
    private LocalDateTime updatedAt;
}
