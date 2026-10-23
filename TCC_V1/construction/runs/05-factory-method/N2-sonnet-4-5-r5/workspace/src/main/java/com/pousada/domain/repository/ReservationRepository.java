package com.pousada.domain.repository;

import com.pousada.domain.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    void update(Reservation reservation);
    Optional<Reservation> findById(long id);
    List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findPaidByNight(LocalDate night);
    List<Reservation> findByCheckInMonth(int year, int month);
}
