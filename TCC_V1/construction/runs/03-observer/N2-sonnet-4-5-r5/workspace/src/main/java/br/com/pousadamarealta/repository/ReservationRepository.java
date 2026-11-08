package br.com.pousadamarealta.repository;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night, ReservationStatus... excludeStatuses);
    List<Reservation> findByNight(LocalDate night);
    List<Reservation> findByCheckInMonth(YearMonth month);
}
