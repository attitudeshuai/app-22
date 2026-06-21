package com.umbrellapoint.dto.borrow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BorrowRecordRequest {
    @NotNull(message = "雨伞ID不能为空")
    private Long umbrellaId;

    @NotNull(message = "借还站点ID不能为空")
    private Long borrowStationId;

    private BigDecimal deposit;
}
