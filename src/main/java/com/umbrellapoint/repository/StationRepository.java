package com.umbrellapoint.repository;

import com.umbrellapoint.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    Page<Station> findByNameContainingOrAddressContaining(String name, String address, Pageable pageable);
    Page<Station> findByIsActive(Boolean isActive, Pageable pageable);
    long countByIsActive(Boolean isActive);
    Optional<Station> findByQrCode(String qrCode);
}
