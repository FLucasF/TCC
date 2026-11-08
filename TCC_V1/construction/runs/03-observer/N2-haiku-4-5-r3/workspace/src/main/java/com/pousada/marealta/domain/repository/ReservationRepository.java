package com.pousada.marealta.domain.repository;

import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.shared.DateRange;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    long nextId();
    void save(Reservation reservation);
    Optional<Reservation> findById(long id);
    List<Reservation> findAll();
    List<Reservation> findByDateRange(DateRange dates);
    List<Reservation> findConfirmedByDate(LocalDate date);
    List<Reservation> findByRoomAndDateRange(int roomNumber, DateRange dates);
    List<Reservation> findByMonth(YearMonth month);
}
