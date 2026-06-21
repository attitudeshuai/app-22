package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.stats.OverviewStats;
import com.umbrellapoint.dto.stats.TrendStats;
import com.umbrellapoint.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计分析", description = "数据统计与趋势分析接口")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "总览统计")
    public ResponseEntity<ApiResponse<OverviewStats>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getOverview()));
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势统计")
    public ResponseEntity<ApiResponse<TrendStats>> getTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(ApiResponse.success(statsService.getTrend(startDate, endDate)));
    }
}
