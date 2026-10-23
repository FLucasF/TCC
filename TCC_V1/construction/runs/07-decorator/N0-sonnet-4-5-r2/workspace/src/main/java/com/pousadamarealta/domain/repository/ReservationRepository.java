package com.pousadamarealta.domain.repository;

import com.pousadamarealta.domain.model.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findPaidReservationsForNight(LocalDate night);
    List<Reservation> findReservationsStartingInMonth(int year, int month);
}
