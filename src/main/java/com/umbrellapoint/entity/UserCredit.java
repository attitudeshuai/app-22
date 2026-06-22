package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_credits")
public class UserCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer score = 100;

    @Column(name = "overdue_count")
    private Integer overdueCount = 0;

    @Column(name = "pending_fees", precision = 10, scale = 2)
    private BigDecimal pendingFees = BigDecimal.ZERO;

    @Column(name = "pending_fee_count")
    private Integer pendingFeeCount = 0;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
