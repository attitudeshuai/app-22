package com.umbrellapoint.dto.station;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationDto {
    private Long id;
    private String name;
    private String address;
    private Long managerId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer capacity;
    private String qrCode;
    private Integer safetyThreshold;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
