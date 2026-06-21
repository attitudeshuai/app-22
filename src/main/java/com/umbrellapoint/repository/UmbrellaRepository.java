package com.umbrellapoint.repository;

import com.umbrellapoint.entity.Umbrella;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UmbrellaRepository extends JpaRepository<Umbrella, Long> {
    Optional<Umbrella> findByCode(String code);
    boolean existsByCode(String code);
    Page<Umbrella> findByCodeContainingOrColorContaining(String code, String color, Pageable pageable);
    Page<Umbrella> findByStationId(Long stationId, Pageable pageable);
    Page<Umbrella> findByStatus(Umbrella.UmbrellaStatus status, Pageable pageable);
    List<Umbrella> findByStationIdAndStatus(Long stationId, Umbrella.UmbrellaStatus status);
    long countByStatus(Umbrella.UmbrellaStatus status);
    long countByStationId(Long stationId);
    long countByStationIdAndStatus(Long stationId, Umbrella.UmbrellaStatus status);
}
