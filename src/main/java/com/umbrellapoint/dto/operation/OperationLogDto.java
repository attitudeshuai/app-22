package com.umbrellapoint.dto.operation;

import com.umbrellapoint.entity.OperationLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDto {

    private Long id;
    private OperationLog.OperationType type;
    private Long relatedId;
    private Long userId;
    private Long stationId;
    private Long umbrellaId;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
}
