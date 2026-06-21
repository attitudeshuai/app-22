package com.umbrellapoint.dto.umbrella;

import com.umbrellapoint.entity.Umbrella;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UmbrellaStatusRequest {
    @NotNull(message = "雨伞状态不能为空")
    private Umbrella.UmbrellaStatus status;
}
