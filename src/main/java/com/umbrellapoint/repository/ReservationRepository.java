package com.umbrellapoint.repository;

import com.umbrellapoint.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByStationId(Long stationId, Pageable pageable);

    Page<Reservation> findByStatus(Reservation.ReservationStatus status, Pageable pageable);

    List<Reservation> findByStatusAndExpireTimeBefore(Reservation.ReservationStatus status, LocalDateTime time);

    List<Reservation> findByStationIdAndStatusOrderByCreatedAtAsc(Long stationId, Reservation.ReservationStatus status);

    Optional<Reservation> findFirstByStationIdAndStatusOrderByCreatedAtAsc(Long stationId, Reservation.ReservationStatus status);

    Optional<Reservation> findByUserIdAndStatus(Long userId, Reservation.ReservationStatus status);

    List<Reservation> findByUmbrellaIdAndStatus(Long umbrellaId, Reservation.ReservationStatus status);

    long countByStationIdAndStatus(Long stationId, Reservation.ReservationStatus status);

    boolean existsByUserIdAndStatusIn(Long userId, List<Reservation.ReservationStatus> statuses);
}
