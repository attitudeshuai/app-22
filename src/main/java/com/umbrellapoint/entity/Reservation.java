package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "umbrella_id", nullable = false)
    private Long umbrellaId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "expected_borrow_start")
    private LocalDateTime expectedBorrowStart;

    @Column(name = "expected_borrow_end")
    private LocalDateTime expectedBorrowEnd;

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    @Column(name = "borrow_record_id")
    private Long borrowRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.Pending;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum ReservationStatus {
        Pending, Active, Completed, Expired, Cancelled
    }
}
