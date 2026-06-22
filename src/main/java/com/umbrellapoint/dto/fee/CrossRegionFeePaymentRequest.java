package com.umbrellapoint.dto.fee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrossRegionFeePaymentRequest {
    @NotNull(message = "费用ID不能为空")
    private Long feeId;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    private String transactionId;

    private BigDecimal amount;
}
