package com.umbrellapoint.dto.umbrella;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UmbrellaRequest {
    @NotBlank(message = "雨伞编号不能为空")
    private String code;

    @NotNull(message = "所属站点ID不能为空")
    private Long stationId;

    private String color;
}
