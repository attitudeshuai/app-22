package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.stats.*;
import com.umbrellapoint.service.DailyStatsService;
import com.umbrellapoint.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计分析", description = "数据统计与趋势分析接口")
public class StatsController {

    private final StatsService statsService;
    private final DailyStatsService dailyStatsService;

    public StatsController(StatsService statsService, DailyStatsService dailyStatsService) {
        this.statsService = statsService;
        this.dailyStatsService = dailyStatsService;
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

    @GetMapping("/stations")
    @Operation(summary = "站点维度实时统计", description = "按借还点维度汇总：借还总量、逾期率、平均借用时长、当前可用数量")
    public ResponseEntity<ApiResponse<List<StationStatsDto>>> getStationStats(
            @RequestParam(required = false) List<Long> stationIds) {
        return ResponseEntity.ok(ApiResponse.success(dailyStatsService.getStationStats(stationIds)));
    }

    @GetMapping("/daily-report")
    @Operation(summary = "获取日报数据", description = "获取指定日期的站点日报，包含环比和同比分析数据")
    public ResponseEntity<ApiResponse<DailyReportDto>> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> stationIds) {
        LocalDate statDate = date != null ? date : LocalDate.now().minusDays(1);
        return ResponseEntity.ok(ApiResponse.success(dailyStatsService.getDailyReport(statDate, stationIds)));
    }

    @GetMapping("/daily-stats")
    @Operation(summary = "按日期范围查询日报明细", description = "按日期和站点范围查询站点日报明细数据")
    public ResponseEntity<ApiResponse<List<StationDailyStatsDto>>> getDailyStatsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> stationIds) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyStatsService.getDailyStatsByDateRange(startDate, endDate, stationIds)));
    }

    @PostMapping("/daily-report/generate")
    @Operation(summary = "手动生成日报", description = "手动触发生成指定日期的日报数据")
    public ResponseEntity<ApiResponse<String>> generateDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyStatsService.generateDailyReport(date);
        return ResponseEntity.ok(ApiResponse.success("日报生成成功"));
    }

    @GetMapping("/daily-report/export")
    @Operation(summary = "导出日报CSV", description = "导出指定日期的日报为CSV文件，用于周会汇报")
    public ResponseEntity<byte[]> exportDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> stationIds) {
        LocalDate statDate = date != null ? date : LocalDate.now().minusDays(1);
        byte[] csvData = dailyStatsService.exportDailyReportToCsv(statDate, stationIds);

        String fileName = "daily_report_" + statDate + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/daily-stats/export")
    @Operation(summary = "导出日期范围日报CSV", description = "导出指定日期范围的站点日报明细为CSV文件")
    public ResponseEntity<byte[]> exportDailyStatsRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> stationIds) {
        byte[] csvData = dailyStatsService.exportDailyStatsRangeToCsv(startDate, endDate, stationIds);

        String fileName = "daily_stats_" + startDate + "_to_" + endDate + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/comparison")
    @Operation(summary = "环比同比分析", description = "获取指定日期的环比（较昨日）和同比（较上周）分析数据")
    public ResponseEntity<ApiResponse<DailyReportDto>> getComparisonAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> stationIds) {
        LocalDate statDate = date != null ? date : LocalDate.now().minusDays(1);
        return ResponseEntity.ok(ApiResponse.success(dailyStatsService.getDailyReport(statDate, stationIds)));
    }
}
