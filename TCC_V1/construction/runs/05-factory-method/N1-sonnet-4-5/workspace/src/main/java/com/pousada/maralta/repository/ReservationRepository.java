package com.pousada.maralta.repository;

import com.pousada.maralta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night);
    List<Reservation> findPaidByNight(LocalDate night);
    List<Reservation> findByCheckInMonth(YearMonth month);
}
