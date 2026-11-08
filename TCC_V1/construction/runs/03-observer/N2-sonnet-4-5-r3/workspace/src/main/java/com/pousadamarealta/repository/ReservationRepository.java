package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    void update(Reservation reservation);
    Optional<Reservation> findById(long id);
    boolean hasConflictingReservation(int roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findByRoomAndDateAndStatuses(int roomNumber, LocalDate date, List<ReservationStatus> statuses);
    List<Reservation> findByCheckInMonthAndStatuses(YearMonth month, List<ReservationStatus> statuses);
}
