package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "station_daily_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stat_date", "station_id"})
})
public class StationDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "station_name", length = 100)
    private String stationName;

    @Column(name = "borrow_count")
    private Integer borrowCount = 0;

    @Column(name = "return_count")
    private Integer returnCount = 0;

    @Column(name = "total_borrow_return")
    private Integer totalBorrowReturn = 0;

    @Column(name = "overdue_count")
    private Integer overdueCount = 0;

    @Column(name = "overdue_rate", precision = 5, scale = 2)
    private BigDecimal overdueRate = BigDecimal.ZERO;

    @Column(name = "avg_borrow_duration_minutes")
    private BigDecimal avgBorrowDurationMinutes = BigDecimal.ZERO;

    @Column(name = "available_umbrellas")
    private Integer availableUmbrellas = 0;

    @Column(name = "total_umbrellas")
    private Integer totalUmbrellas = 0;

    @Column(name = "new_users")
    private Integer newUsers = 0;

    @Column(name = "credit_deduction_count")
    private Integer creditDeductionCount = 0;

    @Column(name = "cross_region_count")
    private Integer crossRegionCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
