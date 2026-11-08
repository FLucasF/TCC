package com.pousadamarealta.domain.repository;

import com.pousadamarealta.domain.model.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findPaidByDate(LocalDate date);
    List<Reservation> findByCheckInMonth(int year, int month);
}
