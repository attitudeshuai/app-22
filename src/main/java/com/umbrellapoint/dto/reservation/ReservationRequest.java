package com.umbrellapoint.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {

    @NotNull(message = "借还点ID不能为空")
    private Long stationId;

    private LocalDateTime expectedBorrowStart;

    private LocalDateTime expectedBorrowEnd;
}
