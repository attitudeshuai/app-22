package com.umbrellapoint.dto.reservation;

import com.umbrellapoint.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    private Long id;
    private Long userId;
    private Long umbrellaId;
    private Long stationId;
    private String stationName;
    private String umbrellaCode;
    private LocalDateTime expectedBorrowStart;
    private LocalDateTime expectedBorrowEnd;
    private LocalDateTime expireTime;
    private Long borrowRecordId;
    private Reservation.ReservationStatus status;
    private Integer queuePosition;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
