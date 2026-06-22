package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrow_records")
public class BorrowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "umbrella_id", nullable = false)
    private Long umbrellaId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "borrow_station_id", nullable = false)
    private Long borrowStationId;

    @Column(name = "return_station_id")
    private Long returnStationId;

    @Column(name = "borrow_time", nullable = false)
    private LocalDateTime borrowTime;

    @Column(name = "return_time")
    private LocalDateTime returnTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.Ongoing;

    @Column(precision = 10, scale = 2)
    private BigDecimal deposit;

    @Column(name = "cross_region_fee_id", unique = true)
    private Long crossRegionFeeId;

    @Column(name = "is_cross_region")
    private Boolean isCrossRegion = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.None;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum BorrowStatus {
        Ongoing, Returned, Overdue
    }

    public enum PaymentStatus {
        None, Pending, Paid, Refunded
    }
}
