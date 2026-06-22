package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.stats.OverviewStats;
import com.umbrellapoint.dto.stats.RejectUserStat;
import com.umbrellapoint.dto.stats.TrendStats;
import com.umbrellapoint.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/frequent-rejects")
    @Operation(summary = "高频被拒用户统计", description = "统计指定时间范围内高频借伞被拒用户，用户ID已匿名化处理")
    public ResponseEntity<ApiResponse<List<RejectUserStat>>> getFrequentRejectUsers(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int topN) {
        return ResponseEntity.ok(ApiResponse.success(statsService.getFrequentRejectUsers(startDate, endDate, topN)));
    }
}
