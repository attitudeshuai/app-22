package com.umbrellapoint.dto.credit;

import com.umbrellapoint.entity.CreditChangeLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditChangeLogDto {
    private Long id;
    private Long userId;
    private Long borrowRecordId;
    private CreditChangeLog.ChangeType changeType;
    private Integer scoreBefore;
    private Integer scoreAfter;
    private Integer scoreChange;
    private Integer overdueDays;
    private String reason;
    private Integer gracePeriodHoursAtTime;
    private Integer penaltyPerDayAtTime;
    private Long operatorId;
    private LocalDateTime createdAt;
}
