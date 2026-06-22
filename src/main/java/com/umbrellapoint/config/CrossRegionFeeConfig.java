package com.umbrellapoint.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "cross-region-fee")
public class CrossRegionFeeConfig {

    private boolean enabled = true;

    private BigDecimal baseFee = new BigDecimal("2.00");

    private BigDecimal perKmFee = new BigDecimal("1.00");

    private BigDecimal minFee = new BigDecimal("2.00");

    private BigDecimal maxFee = new BigDecimal("20.00");

    private BigDecimal freeDistanceKm = new BigDecimal("1.0");

    private Integer settlementPeriodDays = 7;

    private Integer refundValidDays = 3;

    private BigDecimal nearbyStationRadiusKm = new BigDecimal("3.0");

    private Integer nearbyStationLimit = 5;
}
