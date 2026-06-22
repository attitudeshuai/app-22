package com.umbrellapoint.repository;

import com.umbrellapoint.entity.StationDailyStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StationDailyStatsRepository extends JpaRepository<StationDailyStats, Long> {

    Optional<StationDailyStats> findByStatDateAndStationId(LocalDate statDate, Long stationId);

    List<StationDailyStats> findByStatDate(LocalDate statDate);

    List<StationDailyStats> findByStatDateBetween(LocalDate startDate, LocalDate endDate);

    List<StationDailyStats> findByStationIdAndStatDateBetween(Long stationId, LocalDate startDate, LocalDate endDate);

    Page<StationDailyStats> findByStatDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<StationDailyStats> findByStationIdInAndStatDateBetween(List<Long> stationIds, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM StationDailyStats s WHERE s.statDate = :statDate AND (:stationIds IS NULL OR s.stationId IN :stationIds)")
    List<StationDailyStats> findByStatDateAndStationIdIn(LocalDate statDate, List<Long> stationIds);

    @Query("SELECT s FROM StationDailyStats s WHERE s.statDate BETWEEN :startDate AND :endDate AND (:stationIds IS NULL OR s.stationId IN :stationIds) ORDER BY s.statDate ASC, s.stationId ASC")
    List<StationDailyStats> findByDateRangeAndStationIds(LocalDate startDate, LocalDate endDate, List<Long> stationIds);

    boolean existsByStatDate(LocalDate statDate);
}
