package com.umbrellapoint.dto.credit;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreditRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private Integer score = 100;

    private Integer overdueCount = 0;
}
