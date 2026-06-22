package com.umbrellapoint.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_configs")
public class CreditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false, length = 50)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 100)
    private String configValue;

    @Column(name = "config_name", length = 100)
    private String configName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static final String GRACE_PERIOD_HOURS = "grace_period_hours";
    public static final String OVERDUE_PENALTY_PER_DAY = "overdue_penalty_per_day";
    public static final String MAX_OVERDUE_PENALTY = "max_overdue_penalty";
    public static final String OVERDUE_NOTIFY_ENABLED = "overdue_notify_enabled";
}
