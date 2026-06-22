package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperationType type;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "umbrella_id")
    private Long umbrellaId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum OperationType {
        RESERVATION_CREATE,
        RESERVATION_COMPLETE,
        RESERVATION_EXPIRE,
        RESERVATION_CANCEL,
        UMBRELLA_BORROW,
        UMBRELLA_BORROW_REJECT,
        UMBRELLA_RETURN,
        UMBRELLA_LOCK,
        UMBRELLA_UNLOCK,
        QUEUE_NOTIFY
    }
}
