package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findActiveByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findPaidByDate(LocalDate date);
    List<Reservation> findByCheckInMonth(YearMonth month);
}
