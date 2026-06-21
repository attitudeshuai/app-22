package com.umbrellapoint.dto.umbrella;

import com.umbrellapoint.entity.Umbrella;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UmbrellaDto {
    private Long id;
    private String code;
    private Long stationId;
    private String color;
    private Umbrella.UmbrellaStatus status;
    private LocalDateTime createdAt;
}
