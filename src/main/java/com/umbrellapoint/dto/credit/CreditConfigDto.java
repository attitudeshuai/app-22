package com.umbrellapoint.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditConfigDto {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String description;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
