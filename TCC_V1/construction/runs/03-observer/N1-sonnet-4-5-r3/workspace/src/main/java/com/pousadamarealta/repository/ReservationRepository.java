package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByRoomAndPeriod(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findByNightAndStatus(LocalDate night, ReservationStatus... statuses);
    List<Reservation> findByMonthAndStatuses(YearMonth month, ReservationStatus... statuses);
}
