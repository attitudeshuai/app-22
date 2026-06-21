package com.umbrellapoint.dto.station;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StationRequest {
    @NotBlank(message = "站点名称不能为空")
    private String name;

    @NotBlank(message = "站点地址不能为空")
    private String address;

    private Long managerId;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @NotNull(message = "容量不能为空")
    @Positive(message = "容量必须大于0")
    private Integer capacity;

    private Boolean isActive = true;
}
