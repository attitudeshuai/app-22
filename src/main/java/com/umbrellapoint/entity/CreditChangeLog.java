package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_change_logs")
public class CreditChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "borrow_record_id")
    private Long borrowRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private ChangeType changeType;

    @Column(name = "score_before", nullable = false)
    private Integer scoreBefore;

    @Column(name = "score_after", nullable = false)
    private Integer scoreAfter;

    @Column(name = "score_change", nullable = false)
    private Integer scoreChange;

    @Column(name = "overdue_days")
    private Integer overdueDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "grace_period_hours_at_time")
    private Integer gracePeriodHoursAtTime;

    @Column(name = "penalty_per_day_at_time")
    private Integer penaltyPerDayAtTime;

    @Column(name = "operator_id")
    private Long operatorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ChangeType {
        OVERDUE_PENALTY,
        APPEAL_RESTORE,
        MANUAL_ADJUST,
        SCORE_RECOVERY,
        INITIALIZE
    }
}
