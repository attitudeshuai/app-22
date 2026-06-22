package com.umbrellapoint.service;

import com.umbrellapoint.dto.stats.*;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.CreditChangeLog;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.entity.StationDailyStats;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.CreditChangeLogRepository;
import com.umbrellapoint.repository.StationDailyStatsRepository;
import com.umbrellapoint.repository.StationRepository;
import com.umbrellapoint.repository.UmbrellaRepository;
import com.umbrellapoint.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DailyStatsService {

    private static final Logger logger = LoggerFactory.getLogger(DailyStatsService.class);

    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final StationDailyStatsRepository stationDailyStatsRepository;
    private final UserRepository userRepository;
    private final CreditChangeLogRepository creditChangeLogRepository;

    public DailyStatsService(StationRepository stationRepository,
                             UmbrellaRepository umbrellaRepository,
                             BorrowRecordRepository borrowRecordRepository,
                             StationDailyStatsRepository stationDailyStatsRepository,
                             UserRepository userRepository,
                             CreditChangeLogRepository creditChangeLogRepository) {
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.stationDailyStatsRepository = stationDailyStatsRepository;
        this.userRepository = userRepository;
        this.creditChangeLogRepository = creditChangeLogRepository;
    }

    public List<StationStatsDto> getStationStats(List<Long> stationIds) {
        List<Station> stations;
        if (stationIds != null && !stationIds.isEmpty()) {
            stations = stationRepository.findAllById(stationIds);
        } else {
            stations = stationRepository.findAll();
        }

        List<StationStatsDto> result = new ArrayList<>();
        for (Station station : stations) {
            StationStatsDto dto = calculateStationStats(station);
            result.add(dto);
        }

        result.sort((a, b) -> Integer.compare(b.getTotalBorrowReturn(), a.getTotalBorrowReturn()));
        return result;
    }

    private StationStatsDto calculateStationStats(Station station) {
        StationStatsDto dto = new StationStatsDto();
        dto.setStationId(station.getId());
        dto.setStationName(station.getName());
        dto.setStationAddress(station.getAddress());
        dto.setCapacity(station.getCapacity());

        long borrowCount = borrowRecordRepository.countByBorrowStationId(station.getId());
        long returnCount = borrowRecordRepository.countByReturnStationId(station.getId());
        long overdueCount = borrowRecordRepository.countByBorrowStationIdAndStatus(
                station.getId(), BorrowRecord.BorrowStatus.Overdue);

        dto.setBorrowCount((int) borrowCount);
        dto.setReturnCount((int) returnCount);
        dto.setTotalBorrowReturn((int) (borrowCount + returnCount));
        dto.setOverdueCount((int) overdueCount);

        BigDecimal overdueRate = borrowCount > 0
                ? BigDecimal.valueOf(overdueCount)
                        .divide(BigDecimal.valueOf(borrowCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        dto.setOverdueRate(overdueRate.setScale(2, RoundingMode.HALF_UP));

        Double avgDuration = borrowRecordRepository.avgBorrowDurationByStationIdAndTimeRange(
                station.getId(), BorrowRecord.BorrowStatus.Returned,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now());
        dto.setAvgBorrowDurationMinutes(avgDuration != null
                ? BigDecimal.valueOf(avgDuration).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        long availableCount = umbrellaRepository.countByStationIdAndStatus(
                station.getId(), Umbrella.UmbrellaStatus.Available);
        long totalUmbrellaCount = umbrellaRepository.countByStationId(station.getId());
        dto.setAvailableUmbrellas((int) availableCount);
        dto.setTotalUmbrellas((int) totalUmbrellaCount);

        return dto;
    }

    @Transactional
    public void generateDailyReport(LocalDate date) {
        logger.info("开始生成 {} 的站点日报...", date);

        if (stationDailyStatsRepository.existsByStatDate(date)) {
            logger.info("{} 的日报已存在，跳过生成", date);
            return;
        }

        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        List<Station> stations = stationRepository.findAll();
        List<StationDailyStats> statsList = new ArrayList<>();

        Map<Long, Long> borrowCountMap = getBorrowCountMap(startTime, endTime);
        Map<Long, Long> returnCountMap = getReturnCountMap(startTime, endTime);
        Map<Long, Long> overdueCountMap = getOverdueCountMap(startTime, endTime);
        Map<Long, Double> avgDurationMap = getAvgDurationMap(startTime, endTime);

        long totalNewUsers = userRepository.countByCreatedAtBetween(startTime, endTime);

        for (Station station : stations) {
            StationDailyStats stats = new StationDailyStats();
            stats.setStatDate(date);
            stats.setStationId(station.getId());
            stats.setStationName(station.getName());

            long borrowCount = borrowCountMap.getOrDefault(station.getId(), 0L);
            long returnCount = returnCountMap.getOrDefault(station.getId(), 0L);
            long overdueCount = overdueCountMap.getOrDefault(station.getId(), 0L);
            Double avgDuration = avgDurationMap.get(station.getId());

            stats.setBorrowCount((int) borrowCount);
            stats.setReturnCount((int) returnCount);
            stats.setTotalBorrowReturn((int) (borrowCount + returnCount));
            stats.setOverdueCount((int) overdueCount);

            BigDecimal overdueRate = borrowCount > 0
                    ? BigDecimal.valueOf(overdueCount)
                            .divide(BigDecimal.valueOf(borrowCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            stats.setOverdueRate(overdueRate.setScale(2, RoundingMode.HALF_UP));

            stats.setAvgBorrowDurationMinutes(avgDuration != null
                    ? BigDecimal.valueOf(avgDuration).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            long availableCount = umbrellaRepository.countByStationIdAndStatus(
                    station.getId(), Umbrella.UmbrellaStatus.Available);
            long totalUmbrellaCount = umbrellaRepository.countByStationId(station.getId());
            stats.setAvailableUmbrellas((int) availableCount);
            stats.setTotalUmbrellas((int) totalUmbrellaCount);

            long crossRegionCount = borrowRecordRepository.countCrossRegionByStationIdAndTimeRange(
                    station.getId(), startTime, endTime);
            stats.setCrossRegionCount((int) crossRegionCount);

            long creditDeductionCount = creditChangeLogRepository.countDeductionsByStationIdAndTimeRange(
                    CreditChangeLog.ChangeType.OVERDUE_PENALTY, station.getId(), startTime, endTime);
            stats.setCreditDeductionCount((int) creditDeductionCount);

            statsList.add(stats);
        }

        if (!statsList.isEmpty() && totalNewUsers > 0) {
            StationDailyStats firstStation = statsList.get(0);
            firstStation.setNewUsers((int) totalNewUsers);
        }

        stationDailyStatsRepository.saveAll(statsList);
        logger.info("成功生成 {} 的站点日报，共 {} 个站点，新增用户 {} 人", date, statsList.size(), totalNewUsers);
    }

    private Map<Long, Long> getBorrowCountMap(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = borrowRecordRepository.countBorrowsByStationAndTimeRange(startTime, endTime);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : results) {
            Long stationId = (Long) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(stationId, count);
        }
        return map;
    }

    private Map<Long, Long> getReturnCountMap(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = borrowRecordRepository.countReturnsByStationAndTimeRange(startTime, endTime);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : results) {
            Long stationId = (Long) row[0];
            if (stationId != null) {
                Long count = ((Number) row[1]).longValue();
                map.put(stationId, count);
            }
        }
        return map;
    }

    private Map<Long, Long> getOverdueCountMap(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = borrowRecordRepository.countOverdueByBorrowStationAndTimeRange(
                BorrowRecord.BorrowStatus.Overdue, startTime, endTime);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : results) {
            Long stationId = (Long) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(stationId, count);
        }
        return map;
    }

    private Map<Long, Double> getAvgDurationMap(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = borrowRecordRepository.avgBorrowDurationByBorrowStationAndTimeRange(
                BorrowRecord.BorrowStatus.Returned, startTime, endTime);
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : results) {
            Long stationId = (Long) row[0];
            Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            map.put(stationId, avg);
        }
        return map;
    }

    public DailyReportDto getDailyReport(LocalDate date, List<Long> stationIds) {
        List<StationDailyStats> statsList = stationDailyStatsRepository
                .findByDateRangeAndStationIds(date, date, stationIds);

        if (statsList.isEmpty()) {
            generateDailyReport(date);
            statsList = stationDailyStatsRepository
                    .findByDateRangeAndStationIds(date, date, stationIds);
        }

        return buildDailyReportDto(date, statsList);
    }

    private DailyReportDto buildDailyReportDto(LocalDate date, List<StationDailyStats> statsList) {
        DailyReportDto report = new DailyReportDto();
        report.setReportDate(date);
        report.setTotalStations(statsList.size());

        int totalBorrows = 0;
        int totalReturns = 0;
        int totalOverdue = 0;
        int totalAvailable = 0;
        int totalUmbrellas = 0;
        int totalNewUsers = 0;
        int totalCreditDeductions = 0;
        BigDecimal totalDuration = BigDecimal.ZERO;
        int durationCount = 0;

        List<StationDailyStatsDto> stationDtos = new ArrayList<>();
        for (StationDailyStats stats : statsList) {
            totalBorrows += stats.getBorrowCount();
            totalReturns += stats.getReturnCount();
            totalOverdue += stats.getOverdueCount();
            totalAvailable += stats.getAvailableUmbrellas();
            totalUmbrellas += stats.getTotalUmbrellas();
            totalNewUsers += stats.getNewUsers() != null ? stats.getNewUsers() : 0;
            totalCreditDeductions += stats.getCreditDeductionCount() != null ? stats.getCreditDeductionCount() : 0;

            if (stats.getAvgBorrowDurationMinutes() != null
                    && stats.getAvgBorrowDurationMinutes().compareTo(BigDecimal.ZERO) > 0
                    && stats.getReturnCount() > 0) {
                totalDuration = totalDuration.add(
                        stats.getAvgBorrowDurationMinutes().multiply(BigDecimal.valueOf(stats.getReturnCount())));
                durationCount += stats.getReturnCount();
            }

            stationDtos.add(convertToDto(stats));
        }

        report.setTotalBorrows(totalBorrows);
        report.setTotalReturns(totalReturns);
        report.setTotalBorrowReturn(totalBorrows + totalReturns);
        report.setTotalOverdue(totalOverdue);
        report.setTotalAvailableUmbrellas(totalAvailable);
        report.setTotalUmbrellas(totalUmbrellas);
        report.setTotalNewUsers(totalNewUsers);
        report.setTotalCreditDeductions(totalCreditDeductions);

        BigDecimal overallOverdueRate = totalBorrows > 0
                ? BigDecimal.valueOf(totalOverdue)
                        .divide(BigDecimal.valueOf(totalBorrows), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        report.setOverallOverdueRate(overallOverdueRate.setScale(2, RoundingMode.HALF_UP));

        BigDecimal overallAvgDuration = durationCount > 0
                ? totalDuration.divide(BigDecimal.valueOf(durationCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        report.setOverallAvgDurationMinutes(overallAvgDuration);

        report.setStationStats(stationDtos);

        report.setDayOverDay(calculateComparison(date, statsList, 1));
        report.setWeekOverWeek(calculateComparison(date, statsList, 7));

        return report;
    }

    private ComparisonStatsDto calculateComparison(LocalDate currentDate,
                                                    List<StationDailyStats> currentStats,
                                                    int daysAgo) {
        LocalDate comparisonDate = currentDate.minusDays(daysAgo);
        List<Long> stationIds = currentStats.stream()
                .map(StationDailyStats::getStationId)
                .collect(Collectors.toList());

        List<StationDailyStats> comparisonStats = stationDailyStatsRepository
                .findByDateRangeAndStationIds(comparisonDate, comparisonDate, stationIds);

        ComparisonStatsDto dto = new ComparisonStatsDto();
        dto.setComparisonType(daysAgo == 1 ? "day_over_day" : "week_over_week");
        dto.setComparisonPeriod(comparisonDate.toString());

        if (comparisonStats.isEmpty()) {
            dto.setHasData(false);
            dto.setRemark("对比日期 " + comparisonDate + " 无历史数据，跳过对比");
            return dto;
        }

        dto.setHasData(true);
        dto.setRemark("对比正常");

        int currentTotal = currentStats.stream()
                .mapToInt(StationDailyStats::getTotalBorrowReturn).sum();
        int comparisonTotal = comparisonStats.stream()
                .mapToInt(StationDailyStats::getTotalBorrowReturn).sum();
        dto.setTotalBorrowReturnChange(BigDecimal.valueOf(currentTotal - comparisonTotal));
        dto.setTotalBorrowReturnChangeRate(calculateChangeRate(currentTotal, comparisonTotal));

        int currentOverdue = currentStats.stream()
                .mapToInt(StationDailyStats::getOverdueCount).sum();
        int comparisonOverdue = comparisonStats.stream()
                .mapToInt(StationDailyStats::getOverdueCount).sum();
        dto.setOverdueCountChange(BigDecimal.valueOf(currentOverdue - comparisonOverdue));
        dto.setOverdueCountChangeRate(calculateChangeRate(currentOverdue, comparisonOverdue));

        BigDecimal currentOverdueRate = currentTotal > 0
                ? BigDecimal.valueOf(currentOverdue)
                        .divide(BigDecimal.valueOf(currentTotal), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        BigDecimal comparisonOverdueRate = comparisonTotal > 0
                ? BigDecimal.valueOf(comparisonOverdue)
                        .divide(BigDecimal.valueOf(comparisonTotal), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        dto.setOverdueRateChange(currentOverdueRate.subtract(comparisonOverdueRate).setScale(2, RoundingMode.HALF_UP));

        BigDecimal currentAvg = calculateWeightedAvgDuration(currentStats);
        BigDecimal comparisonAvg = calculateWeightedAvgDuration(comparisonStats);
        dto.setAvgDurationChange(currentAvg.subtract(comparisonAvg).setScale(2, RoundingMode.HALF_UP));
        dto.setAvgDurationChangeRate(calculateChangeRate(currentAvg.doubleValue(), comparisonAvg.doubleValue()));

        int currentAvailable = currentStats.stream()
                .mapToInt(StationDailyStats::getAvailableUmbrellas).sum();
        int comparisonAvailable = comparisonStats.stream()
                .mapToInt(StationDailyStats::getAvailableUmbrellas).sum();
        dto.setAvailableUmbrellasChange(BigDecimal.valueOf(currentAvailable - comparisonAvailable));
        dto.setAvailableUmbrellasChangeRate(calculateChangeRate(currentAvailable, comparisonAvailable));

        return dto;
    }

    private BigDecimal calculateWeightedAvgDuration(List<StationDailyStats> statsList) {
        BigDecimal totalDuration = BigDecimal.ZERO;
        int totalReturns = 0;
        for (StationDailyStats stats : statsList) {
            if (stats.getAvgBorrowDurationMinutes() != null && stats.getReturnCount() > 0) {
                totalDuration = totalDuration.add(
                        stats.getAvgBorrowDurationMinutes()
                                .multiply(BigDecimal.valueOf(stats.getReturnCount())));
                totalReturns += stats.getReturnCount();
            }
        }
        return totalReturns > 0
                ? totalDuration.divide(BigDecimal.valueOf(totalReturns), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal calculateChangeRate(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - previous)
                .divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangeRate(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - previous)
                .divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StationDailyStatsDto convertToDto(StationDailyStats entity) {
        StationDailyStatsDto dto = new StationDailyStatsDto();
        dto.setId(entity.getId());
        dto.setStatDate(entity.getStatDate());
        dto.setStationId(entity.getStationId());
        dto.setStationName(entity.getStationName());
        dto.setBorrowCount(entity.getBorrowCount());
        dto.setReturnCount(entity.getReturnCount());
        dto.setTotalBorrowReturn(entity.getTotalBorrowReturn());
        dto.setOverdueCount(entity.getOverdueCount());
        dto.setOverdueRate(entity.getOverdueRate());
        dto.setAvgBorrowDurationMinutes(entity.getAvgBorrowDurationMinutes());
        dto.setAvailableUmbrellas(entity.getAvailableUmbrellas());
        dto.setTotalUmbrellas(entity.getTotalUmbrellas());
        dto.setNewUsers(entity.getNewUsers());
        dto.setCreditDeductionCount(entity.getCreditDeductionCount());
        dto.setCrossRegionCount(entity.getCrossRegionCount());
        return dto;
    }

    public List<StationDailyStatsDto> getDailyStatsByDateRange(LocalDate startDate, LocalDate endDate,
                                                               List<Long> stationIds) {
        List<StationDailyStats> statsList = stationDailyStatsRepository
                .findByDateRangeAndStationIds(startDate, endDate, stationIds);
        return statsList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public byte[] exportDailyReportToCsv(LocalDate date, List<Long> stationIds) {
        DailyReportDto report = getDailyReport(date, stationIds);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.println('\ufeff' + "站点日报 - " + date);
            writer.println();

            writer.println("汇总数据");
            writer.println("站点总数," + report.getTotalStations());
            writer.println("新增用户数," + report.getTotalNewUsers());
            writer.println("信用扣减次数," + report.getTotalCreditDeductions());
            writer.println("借还总量," + report.getTotalBorrowReturn());
            writer.println("借出数量," + report.getTotalBorrows());
            writer.println("归还数量," + report.getTotalReturns());
            writer.println("逾期数量," + report.getTotalOverdue());
            writer.println("整体逾期率(%)," + report.getOverallOverdueRate());
            writer.println("平均借用时长(分钟)," + report.getOverallAvgDurationMinutes());
            writer.println("可用雨伞总数," + report.getTotalAvailableUmbrellas());
            writer.println("雨伞总数," + report.getTotalUmbrellas());
            writer.println();

            if (report.getDayOverDay() != null) {
                writer.println("环比(较昨日) - " + report.getDayOverDay().getComparisonPeriod());
                if (Boolean.TRUE.equals(report.getDayOverDay().getHasData())) {
                    writer.println("指标,当前值,变化量,变化率(%)");
                    writer.println("借还总量," + report.getTotalBorrowReturn() + ","
                            + report.getDayOverDay().getTotalBorrowReturnChange() + ","
                            + report.getDayOverDay().getTotalBorrowReturnChangeRate());
                    writer.println("逾期数量," + report.getTotalOverdue() + ","
                            + report.getDayOverDay().getOverdueCountChange() + ","
                            + report.getDayOverDay().getOverdueCountChangeRate());
                    writer.println("逾期率(%)," + report.getOverallOverdueRate() + ","
                            + report.getDayOverDay().getOverdueRateChange() + ",-");
                } else {
                    writer.println("状态," + report.getDayOverDay().getRemark());
                }
                writer.println();
            }

            if (report.getWeekOverWeek() != null) {
                writer.println("同比(较上周) - " + report.getWeekOverWeek().getComparisonPeriod());
                if (Boolean.TRUE.equals(report.getWeekOverWeek().getHasData())) {
                    writer.println("指标,当前值,变化量,变化率(%)");
                    writer.println("借还总量," + report.getTotalBorrowReturn() + ","
                            + report.getWeekOverWeek().getTotalBorrowReturnChange() + ","
                            + report.getWeekOverWeek().getTotalBorrowReturnChangeRate());
                    writer.println("逾期数量," + report.getTotalOverdue() + ","
                            + report.getWeekOverWeek().getOverdueCountChange() + ","
                            + report.getWeekOverWeek().getOverdueCountChangeRate());
                    writer.println("逾期率(%)," + report.getOverallOverdueRate() + ","
                            + report.getWeekOverWeek().getOverdueRateChange() + ",-");
                } else {
                    writer.println("状态," + report.getWeekOverWeek().getRemark());
                }
                writer.println();
            }

            writer.println("站点明细");
            writer.println("站点ID,站点名称,借出数量,归还数量,借还总量,逾期数量,逾期率(%),平均借用时长(分钟),可用雨伞,雨伞总数,跨区域借还数,信用扣减次数");

            for (StationDailyStatsDto stat : report.getStationStats()) {
                writer.println(stat.getStationId() + ","
                        + stat.getStationName() + ","
                        + stat.getBorrowCount() + ","
                        + stat.getReturnCount() + ","
                        + stat.getTotalBorrowReturn() + ","
                        + stat.getOverdueCount() + ","
                        + stat.getOverdueRate() + ","
                        + stat.getAvgBorrowDurationMinutes() + ","
                        + stat.getAvailableUmbrellas() + ","
                        + stat.getTotalUmbrellas() + ","
                        + stat.getCrossRegionCount() + ","
                        + stat.getCreditDeductionCount());
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败", e);
        }
    }

    public byte[] exportDailyStatsRangeToCsv(LocalDate startDate, LocalDate endDate, List<Long> stationIds) {
        List<StationDailyStatsDto> statsList = getDailyStatsByDateRange(startDate, endDate, stationIds);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.println('\ufeff' + "站点日报明细 - " + startDate + " 至 " + endDate);
            writer.println();

            writer.println("统计日期,站点ID,站点名称,借出数量,归还数量,借还总量,逾期数量,逾期率(%),平均借用时长(分钟),可用雨伞,雨伞总数,跨区域借还数,信用扣减次数");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (StationDailyStatsDto stat : statsList) {
                writer.println(stat.getStatDate().format(dateFormatter) + ","
                        + stat.getStationId() + ","
                        + stat.getStationName() + ","
                        + stat.getBorrowCount() + ","
                        + stat.getReturnCount() + ","
                        + stat.getTotalBorrowReturn() + ","
                        + stat.getOverdueCount() + ","
                        + stat.getOverdueRate() + ","
                        + stat.getAvgBorrowDurationMinutes() + ","
                        + stat.getAvailableUmbrellas() + ","
                        + stat.getTotalUmbrellas() + ","
                        + stat.getCrossRegionCount() + ","
                        + stat.getCreditDeductionCount());
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败", e);
        }
    }
}
