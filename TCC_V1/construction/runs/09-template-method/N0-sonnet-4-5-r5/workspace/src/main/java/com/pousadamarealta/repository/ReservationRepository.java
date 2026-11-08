package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    void update(Reservation reservation);
    List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findPaidByNight(LocalDate date);
    List<Reservation> findByMonth(YearMonth month);
}
