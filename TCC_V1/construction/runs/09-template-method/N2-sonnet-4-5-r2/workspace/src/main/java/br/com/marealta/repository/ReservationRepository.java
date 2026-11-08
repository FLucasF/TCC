package br.com.marealta.repository;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    void update(Reservation reservation);
    List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut);
    List<Reservation> findByStatusIn(List<ReservationStatus> statuses);
    List<Reservation> findPaidByDate(LocalDate date);
    List<Reservation> findByCheckInMonth(YearMonth month);
}
