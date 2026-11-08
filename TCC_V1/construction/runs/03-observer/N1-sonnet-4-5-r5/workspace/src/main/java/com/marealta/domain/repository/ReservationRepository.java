package com.marealta.domain.repository;

import com.marealta.domain.model.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId);
    List<Reservation> findByNight(LocalDate night);
    List<Reservation> findByCheckInMonth(YearMonth month);
}
