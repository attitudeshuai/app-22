package com.umbrellapoint.service;

import com.umbrellapoint.dto.stats.OverviewStats;
import com.umbrellapoint.dto.stats.RejectUserStat;
import com.umbrellapoint.dto.stats.TrendStats;
import com.umbrellapoint.entity.BorrowRecord;
import com.umbrellapoint.entity.OperationLog;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.repository.BorrowRecordRepository;
import com.umbrellapoint.repository.OperationLogRepository;
import com.umbrellapoint.repository.StationRepository;
import com.umbrellapoint.repository.UmbrellaRepository;
import com.umbrellapoint.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatsService {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final OperationLogRepository operationLogRepository;

    public StatsService(UserRepository userRepository,
                        StationRepository stationRepository,
                        UmbrellaRepository umbrellaRepository,
                        BorrowRecordRepository borrowRecordRepository,
                        OperationLogRepository operationLogRepository) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.operationLogRepository = operationLogRepository;
    }

    public OverviewStats getOverview() {
        OverviewStats stats = new OverviewStats();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalStations(stationRepository.count());
        stats.setActiveStations(stationRepository.countByIsActive(true));
        stats.setTotalUmbrellas(umbrellaRepository.count());
        stats.setAvailableUmbrellas(umbrellaRepository.countByStatus(Umbrella.UmbrellaStatus.Available));
        stats.setBorrowedUmbrellas(umbrellaRepository.countByStatus(Umbrella.UmbrellaStatus.Borrowed));
        stats.setOngoingBorrows(borrowRecordRepository.countByStatus(BorrowRecord.BorrowStatus.Ongoing));
        stats.setReturnedBorrows(borrowRecordRepository.countByStatus(BorrowRecord.BorrowStatus.Returned));
        stats.setOverdueBorrows(borrowRecordRepository.countByStatus(BorrowRecord.BorrowStatus.Overdue));

        Map<String, Long> umbrellaStatus = new LinkedHashMap<>();
        for (Umbrella.UmbrellaStatus status : Umbrella.UmbrellaStatus.values()) {
            umbrellaStatus.put(status.name(), umbrellaRepository.countByStatus(status));
        }
        stats.setUmbrellaStatusDistribution(umbrellaStatus);

        Map<String, Long> borrowStatus = new LinkedHashMap<>();
        for (BorrowRecord.BorrowStatus status : BorrowRecord.BorrowStatus.values()) {
            borrowStatus.put(status.name(), borrowRecordRepository.countByStatus(status));
        }
        stats.setBorrowStatusDistribution(borrowStatus);

        return stats;
    }

    public TrendStats getTrend(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate, formatter).atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null
                ? LocalDate.parse(endDate, formatter).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<Object[]> results = borrowRecordRepository.countByDateRange(start, end);
        List<Map<String, Object>> dailyBorrows = new ArrayList<>();
        long total = 0;

        for (Object[] row : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row[0].toString());
            item.put("count", ((Number) row[1]).longValue());
            dailyBorrows.add(item);
            total += ((Number) row[1]).longValue();
        }

        TrendStats stats = new TrendStats();
        stats.setStartDate(start.format(formatter));
        stats.setEndDate(end.format(formatter));
        stats.setDailyBorrows(dailyBorrows);
        stats.setTotalBorrowsInRange(total);
        return stats;
    }

    public List<RejectUserStat> getFrequentRejectUsers(String startDate, String endDate, int topN) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate, formatter).atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null
                ? LocalDate.parse(endDate, formatter).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<Object[]> results = operationLogRepository.countRejectsByUserIdAndDateRange(
                OperationLog.OperationType.UMBRELLA_BORROW_REJECT, start, end);

        List<RejectUserStat> stats = new ArrayList<>();
        int limit = Math.min(topN, results.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = results.get(i);
            Long userId = (Long) row[0];
            Long rejectCount = ((Number) row[1]).longValue();
            Long anonymizedId = anonymizeUserId(userId);
            stats.add(new RejectUserStat(anonymizedId, rejectCount));
        }
        return stats;
    }

    private Long anonymizeUserId(Long userId) {
        if (userId == null) return null;
        return (long) (userId.hashCode() & 0xfffffffL);
    }
}
